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

  val lotteryTable = new LotteryTable(profile)
  lotteryTable.createTableIfNotExists(db).onComplete {
    case scala.util.Success(_) =>
      println("Lottery tables were successfully created or already exist.")
    case scala.util.Failure(exception) =>
      println(s"Error creating Lottery tables: ${exception.getMessage}")
  }
  val lotteryRepo = new SlickLotteryRepository(db, lotteryTable)

  val ballotTable = new BallotTable(profile)
  ballotTable.createTableIfNotExists(db).onComplete {
    case scala.util.Success(_) =>
      println("Lottery tables were successfully created or already exist.")
    case scala.util.Failure(exception) =>
      println(s"Error creating Lottery tables: ${exception.getMessage}")
  }
  val ballotRepo = new SlickBallotRepository(db, ballotTable)

  val lotteryService = new LotteryService(lotteryRepo, ballotRepo)
  val lotteryRoutes = new LotteryRoutes(lotteryService, jwtAuthService)

  val userTable = new UserTable(profile)
  userTable.createTableIfNotExists(db).onComplete {
    case scala.util.Success(_) =>
      println("User tables were successfully created or already exist.")
    case scala.util.Failure(exception) =>
      println(s"Error creating User tables: ${exception.getMessage}")
  }
  val userRepo = new SlickUserRepository(db, userTable)
  val userService = new UserService(userRepo, jwtAuthService)
  val userRoutes = new UserRoutes(userService, jwtAuthService)

  val scheduler = new LotteryScheduler(lotteryService)
  scheduler.startScheduler()


  val allRoutes = lotteryRoutes.routes ~ userRoutes.routes

  val serverBinding = Http().bindAndHandle(allRoutes, "localhost", 8080)
  serverBinding.onComplete {
    case Success(_) => println("Server started at http://localhost:8080")
    case Failure(exception) => println(s"Failed to start server: ${exception.getMessage}")
  }


}
