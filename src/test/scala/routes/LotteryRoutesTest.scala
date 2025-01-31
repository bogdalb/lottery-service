package routes

import akka.http.scaladsl.model.StatusCodes.{Forbidden, OK, Unauthorized}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.LotteryService
import auth.JwtAuth
import models.dto.{SubmitBallotsRequest, CreateLotteryRequest, СreateLotteryResponse}
import org.scalamock.scalatest.MockFactory
import utils.JsonSupport
import models.{Lottery, LotteryStatus}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class LotteryRoutesTest extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest with MockFactory with JsonSupport {

  val mockLotteryService: LotteryService = mock[LotteryService]
  val mockJwtAuthService: JwtAuth = mock[JwtAuth]

  val adminToken = "Bearer AdminToken"
  val userToken =  "Bearer UserToken"
  val invalidToken = "Bearer InvalidToken"

  val lotteryRoutes: Route = new LotteryRoutes(mockLotteryService, mockJwtAuthService)(ExecutionContext.global).routes

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(5.seconds)

  val lotteryId = UUID.randomUUID()
  val lottery = Lottery(lotteryId, LocalDate.now(), LotteryStatus.Active, None)
  val createLotteryRequest = CreateLotteryRequest(lottery.drawDate)
  val createLotteryResponse = СreateLotteryResponse(lottery.id)
  val submitBallotsRequest = SubmitBallotsRequest(lotteryId, 5)
  val ballotIds = Seq.fill(submitBallotsRequest.ballotsNumber)(UUID.randomUUID())
  val userId = UUID.randomUUID()

  "LotteryRoutes" should {

    "POST /lotteries " should {

      "allow admin to create a lottery" in {
        (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
        (mockLotteryService.addLottery _).expects(*).returning(Future.successful(Right(createLotteryResponse)))

        Post("/lotteries", createLotteryRequest) ~> addHeader("Authorization", adminToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[СreateLotteryResponse] shouldBe createLotteryResponse
        }
      }

      "deny non-admin users to create a lottery" in {
        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))

        Post("/lotteries", createLotteryRequest) ~> addHeader("Authorization", userToken) ~> lotteryRoutes ~> check {
          status shouldBe Forbidden
        }
      }
    }

    "GET /lotteries" should {

      "allow admin or user to get lotteries with status filter" in {

        val lottery1 = Lottery(UUID.randomUUID(), LocalDate.now(), LotteryStatus.Active, None)
        val lottery2 = Lottery(UUID.randomUUID(), LocalDate.now(), LotteryStatus.Active, None)

        (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
        (mockLotteryService.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(Right(Seq(lottery1, lottery2))))

        Get("/lotteries?status=Active") ~> addHeader("Authorization", adminToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[Seq[Lottery]] shouldBe Seq(lottery1, lottery2)
        }

        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))
        (mockLotteryService.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(Right(Seq(lottery1, lottery2))))

        Get("/lotteries?status=Active") ~> addHeader("Authorization", userToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
        }
      }
    }

    "GET /lotteries/:id" should {

      "allow admin or user to get lottery by id" in {
        (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
        (mockLotteryService.getLotteryById _).expects(lotteryId).returning(Future.successful(Right(lottery)))

        Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", adminToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[Lottery] shouldBe lottery
        }

        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))
        (mockLotteryService.getLotteryById _).expects(lotteryId).returning(Future.successful(Right(lottery)))

        Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", userToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[Lottery] shouldBe lottery
        }
      }

      "deny access to get lottery by id without valid token" in {
        Get(s"/lotteries/$lotteryId") ~> lotteryRoutes ~> check {
          status shouldBe Unauthorized
        }

        (mockJwtAuthService.decodeToken _).expects(invalidToken).returning(Failure(new IllegalArgumentException(new RuntimeException)))

        Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", invalidToken) ~> lotteryRoutes ~> check {
          status shouldBe Forbidden
        }
      }
    }

    "POST /lotteries/ballots" should {

      "allow user to add ballots to a lottery" in {
        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((userId, "user")))
        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((userId, "user")))
        (mockLotteryService.addBallotsToLottery _).expects(lotteryId, userId, submitBallotsRequest.ballotsNumber).returning(Future.successful(Right(ballotIds)))

        Post("/lotteries/ballots", submitBallotsRequest) ~> addHeader("Authorization", userToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
        }
      }

      "deny adding ballots with invalid token" in {
        (mockJwtAuthService.decodeToken _).expects(invalidToken).returning(Failure(new IllegalArgumentException("Invalid token")))

        Post("/lotteries/ballots", submitBallotsRequest) ~> addHeader("Authorization", invalidToken) ~> lotteryRoutes ~> check {
          status shouldBe Forbidden
        }
      }
    }

    "GET /lotteries with drawDate filter" should {

      "allow admin or user to filter lotteries by drawDate" in {
        val drawDate = LocalDate.now()
        val lottery1 = Lottery(UUID.randomUUID(), drawDate, LotteryStatus.Active, None)
        val lottery2 = Lottery(UUID.randomUUID(), drawDate, LotteryStatus.Closed, None)

        (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
        (mockLotteryService.listLotteries _).expects(None, Some(drawDate)).returning(Future.successful(Right(Seq(lottery1, lottery2))))

        Get(s"/lotteries?drawDate=$drawDate") ~> addHeader("Authorization", adminToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[Seq[Lottery]] shouldBe Seq(lottery1, lottery2)
        }

        (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))
        (mockLotteryService.listLotteries _).expects(None, Some(drawDate)).returning(Future.successful(Right(Seq(lottery1, lottery2))))

        Get(s"/lotteries?drawDate=$drawDate") ~> addHeader("Authorization", userToken) ~> lotteryRoutes ~> check {
          status shouldBe OK
          responseAs[Seq[Lottery]] shouldBe Seq(lottery1, lottery2)
        }
      }
    }
  }

}
