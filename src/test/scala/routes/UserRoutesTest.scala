package routes

import akka.http.scaladsl.model.StatusCodes.{Forbidden, OK}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import services.UserService
import auth.JwtAuth
import models.dto.{ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegistrationResponse}
import models.{User, UserRole}

import scala.concurrent.Future
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import utils.JsonSupport

import java.util.UUID
import scala.util.Success

class UserRoutesTest extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest with MockFactory with JsonSupport {

  val adminToken = "AdminToken"
  val userToken = "UserToken"

  val mockService = mock[UserService]
  val mockJwtAuthService: JwtAuth = mock[JwtAuth]
  val userRoutes = new UserRoutes(mockService, mockJwtAuthService)


  "POST /admin/register" should {
    "successfully register an admin user" in {
      val request = RegisterRequest("admin@example.com", "password123")

      (mockService.register _).expects(request, UserRole.Admin).returning(Future.successful(Right(RegistrationResponse("admin@example.com"))))

      Post("/admin/register", request) ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[RegistrationResponse].email shouldEqual "admin@example.com"
      }
    }

    "return error when registration fails" in {
      val request = RegisterRequest("admin@example.com", "password123")

      (mockService.register _).expects(request, UserRole.Admin).returning(Future.successful(Left(ErrorResponse("Registration failed"))))

      Post("/admin/register", request) ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[ErrorResponse].error shouldEqual "Registration failed"
      }
    }
  }

  "POST /user/register" should {
    "successfully register a user" in {
      val request = RegisterRequest("user@example.com", "password123")

      (mockService.register _).expects(request, UserRole.User).returning(Future.successful(Right(RegistrationResponse("user@example.com"))))

      Post("/user/register", request) ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[RegistrationResponse].email shouldEqual "user@example.com"
      }
    }
  }

  "POST /user/login" should {
    "successfully log in a user" in {
      val request = LoginRequest("user@example.com", "password123")
      val loginResponse = LoginResponse("jwt-token")

      (mockService.login _).expects(request).returning(Future.successful(Right(loginResponse)))

      Post("/user/login", request) ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[LoginResponse].token shouldEqual "jwt-token"
      }
    }

    "return error when credentials are invalid" in {
      val request = LoginRequest("invalid@example.com", "wrongPassword")

      (mockService.login _).expects(request).returning(Future.successful(Left(ErrorResponse("Invalid credentials"))))

      Post("/user/login", request) ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[ErrorResponse].error shouldEqual "Invalid credentials"
      }
    }
  }

  "GET /users/list" should {
    "return a list of users for an admin" in {
      val users = Seq(User(UUID.randomUUID(), "user1@example.com", "passwordHash", UserRole.User))
      (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
      (mockService.listUsers _).expects(100, 0).returning(Future.successful(Right(users)))

      Get("/users/list?limit=100&offset=0") ~> addHeader("Authorization", s"Bearer $adminToken") ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[Seq[User]] shouldEqual users
      }
    }

    "return error when listUsers fails" in {
      (mockJwtAuthService.decodeToken _).expects(adminToken).returning(Success((UUID.randomUUID(), "admin")))
      (mockService.listUsers _).expects(100, 0).returning(Future.successful(Left(ErrorResponse("Failed to list users"))))

      Get("/users/list?limit=100&offset=0") ~> addHeader("Authorization", s"Bearer $adminToken") ~> userRoutes.routes ~> check {
        status shouldBe OK
        responseAs[ErrorResponse].error shouldEqual "Failed to list users"
      }
    }

    "deny non-admin users to call listUsers" in {
      (mockJwtAuthService.decodeToken _).expects(userToken).returning(Success((UUID.randomUUID(), "user")))


      Get("/users/list?limit=100&offset=0") ~> addHeader("Authorization", s"Bearer $userToken") ~> userRoutes.routes ~> check {
        status shouldBe Forbidden
      }
    }
  }
}

