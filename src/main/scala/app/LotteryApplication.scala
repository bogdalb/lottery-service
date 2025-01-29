package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import auth.JwtAuthConfiguration
import com.typesafe.config.{Config, ConfigFactory}
import persistence.DatabaseModule
import persistence.repositories.{SlickBallotRepository, SlickLotteryRepository, SlickUserRepository}
import persistence.tables.{BallotTable, LotteryTable, UserTable}
import routes.{LotteryRoutes, UserRoutes}
import services.{LotteryService, UserService}
import slick.jdbc.SQLiteProfile.api._
import auth.JwtAuthImpl
import jobs.LotteryScheduler

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object LotteryApplication extends App with DatabaseModule {
  implicit val system: ActorSystem = ActorSystem("lottery-service")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val db: slick.jdbc.SQLiteProfile.backend.Database = Database.forConfig("sqlite")
  private val config: Config = ConfigFactory.load()

  val jwtAuthService = JwtAuthImpl(JwtAuthConfiguration.fromConfig(config))

  val (lotteryRepo, ballotRepo, userRepo) = initializeRepositories()

  val lotteryService = new LotteryService(lotteryRepo, ballotRepo)
  val userService = new UserService(userRepo, jwtAuthService)

  val lotteryRoutes = new LotteryRoutes(lotteryService, jwtAuthService)
  val userRoutes = new UserRoutes(userService, jwtAuthService)

  val scheduler = new LotteryScheduler(lotteryService)
  scheduler.startScheduler()

  val allRoutes = lotteryRoutes.routes ~ userRoutes.routes
  startServer(allRoutes)

  private def initializeRepositories(): (SlickLotteryRepository, SlickBallotRepository, SlickUserRepository) = {
    val lotteryTable = new LotteryTable(profile)
    createTableIfNotExists("Lottery", lotteryTable.createTableIfNotExists(db))
    val lotteryRepo = new SlickLotteryRepository(db, lotteryTable)

    val ballotTable = new BallotTable(profile)
    createTableIfNotExists("Ballot", ballotTable.createTableIfNotExists(db))
    val ballotRepo = new SlickBallotRepository(db, ballotTable)

    val userTable = new UserTable(profile)
    createTableIfNotExists("User", userTable.createTableIfNotExists(db))
    val userRepo = new SlickUserRepository(db, userTable)

    (lotteryRepo, ballotRepo, userRepo)
  }

  private def createTableIfNotExists(tableName: String, creationAction: => scala.concurrent.Future[Unit]): Unit = {
    creationAction.onComplete {
      case Success(_) => println(s"$tableName tables were successfully created or already exist.")
      case Failure(exception) => println(s"Error creating $tableName tables: ${exception.getMessage}")
    }
  }

  private def startServer(routes: akka.http.scaladsl.server.Route): Unit = {
    val serverBinding = Http().bindAndHandle(routes, "localhost", 8080)
    serverBinding.onComplete {
      case Success(_) => println("Server started at http://localhost:8080")
      case Failure(exception) => println(s"Failed to start server: ${exception.getMessage}")
    }
  }
}
