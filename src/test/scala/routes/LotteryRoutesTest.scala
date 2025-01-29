package routes

import akka.http.scaladsl.model.StatusCodes.{Forbidden, OK, Unauthorized}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import services.LotteryService
import auth.JwtAuth
import models.dto.{СreateLotteryRequest, СreateLotteryResponse}
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

  val adminToken = "AdminToken"
  val userToken =  "UserToken"
  val invalidToken = "InvalidToken"

  val lotteryRoutes: Route = new LotteryRoutes(mockLotteryService, mockJwtAuthService)(ExecutionContext.global).routes

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(5.seconds)

  val lotteryId = UUID.randomUUID()
  val lottery = Lottery(lotteryId, LocalDate.now(), LotteryStatus.Active, None)
  val createLotteryRequest = СreateLotteryRequest(lottery.drawDate)
  val createLotteryResponse = СreateLotteryResponse(lottery.id)

  "LotteryRoutes" should {

    "allow admin to create a lottery" in {
      (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
      (mockLotteryService.addLottery _).expects(*).returning(Future.successful(Right(createLotteryResponse)))

      Post("/lotteries", createLotteryRequest) ~> addHeader("Authorization", s"Bearer $adminToken") ~> lotteryRoutes ~> check {
        status shouldBe OK
        responseAs[СreateLotteryResponse] shouldBe createLotteryResponse
      }
    }

    "deny non-admin users to create a lottery" in {
      (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))

      Post("/lotteries", createLotteryRequest) ~> addHeader("Authorization", s"Bearer $userToken") ~> lotteryRoutes ~> check {
        status shouldBe Forbidden
      }
    }

    "allow admin or user to get lotteries with status filter" in {

      val lottery1 = Lottery(UUID.randomUUID(), LocalDate.now(), LotteryStatus.Active, None)
      val lottery2 = Lottery(UUID.randomUUID(), LocalDate.now(), LotteryStatus.Active, None)

      (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
      (mockLotteryService.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(Right(Seq(lottery1, lottery2))))

      Get("/lotteries?status=Active") ~> addHeader("Authorization", s"Bearer $adminToken") ~> lotteryRoutes ~> check {
        status shouldBe OK
        responseAs[Seq[Lottery]] shouldBe Seq(lottery1, lottery2)
      }

      (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))
      (mockLotteryService.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(Right(Seq(lottery1, lottery2))))

      Get("/lotteries?status=Active") ~> addHeader("Authorization", s"Bearer $userToken") ~> lotteryRoutes ~> check {
        status shouldBe OK
      }
    }

    "allow admin or user to get lottery by id" in {

      (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
      (mockLotteryService.getLotteryById _).expects(lotteryId).returning(Future.successful(Right(lottery)))

      Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", s"Bearer $adminToken") ~> lotteryRoutes ~> check {
        status shouldBe OK
        responseAs[Lottery] shouldBe lottery
      }

      (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))
      (mockLotteryService.getLotteryById _).expects(lotteryId).returning(Future.successful(Right(lottery)))

      Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", s"Bearer $userToken") ~> lotteryRoutes ~> check {
        status shouldBe OK
        responseAs[Lottery] shouldBe lottery
      }
    }

    "deny access to get lottery by id without valid token" in {
      Get(s"/lotteries/$lotteryId") ~> lotteryRoutes ~> check {
        status shouldBe Unauthorized
      }

      (mockJwtAuthService.decodeToken _).expects(invalidToken).returning(Failure(new IllegalArgumentException(new RuntimeException)))

      Get(s"/lotteries/$lotteryId") ~> addHeader("Authorization", s"Bearer $invalidToken") ~> lotteryRoutes ~> check {
        status shouldBe Forbidden
      }
    }
  }
}
