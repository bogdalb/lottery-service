package services

import auth.JwtAuth
import models.dto.{ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegistrationResponse, UserInfo}
import models.{User, UserRole}
import org.mindrot.jbcrypt.BCrypt
import persistence.repositories.UserRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.util.UUID

class UserServiceTest extends AnyWordSpec with Matchers with ScalaFutures with MockFactory {

  val mockRepo: UserRepository = mock[UserRepository]
  val mockJwtAuth: JwtAuth = mock[JwtAuth]
  val userService: UserService = new UserService(mockRepo, mockJwtAuth)

  "UserService" when {

    "registering a user" should {

      "successfully register a user" in {
        val registerRequest = RegisterRequest("test@example.com", "password123")
        val role = UserRole.User
        val user = User(UUID.randomUUID(), "test@example.com", BCrypt.hashpw("password123", BCrypt.gensalt()), role, LocalDateTime.now())

        (mockRepo.add _).expects(*).returning(Future.successful(1))

        val result = userService.register(registerRequest, role).futureValue

        result shouldBe Right(RegistrationResponse(user.email))
      }

      "fail when repository throws an exception" in {
        val registerRequest = RegisterRequest("test@example.com", "password123")
        val role = UserRole.User

        (mockRepo.add _).expects(*).returning(Future.failed(new Exception("DB error")))

        val result = userService.register(registerRequest, role).futureValue

        result shouldBe Left(ErrorResponse("Registration failed: DB error"))
      }
    }

    "logging in a user" should {

      "successfully log in with correct credentials" in {
        val loginRequest = LoginRequest("test@example.com", "password123")
        val user = User(UUID.randomUUID(), "test@example.com", BCrypt.hashpw("password123", BCrypt.gensalt()), UserRole.User, LocalDateTime.now())
        val token = "jwt-token"

        (mockRepo.getByEmail _).expects("test@example.com").returning(Future.successful(Some(user)))
        (mockJwtAuth.createToken _).expects(user.id, user.role).returning(token)

        val result = userService.login(loginRequest).futureValue

        result shouldBe Right(LoginResponse(token))
      }

      "fail with invalid credentials" in {
        val loginRequest = LoginRequest("test@example.com", "wrongpassword")
        val user = User(UUID.randomUUID(), "test@example.com", BCrypt.hashpw("password123", BCrypt.gensalt()), UserRole.User, LocalDateTime.now())

        (mockRepo.getByEmail _).expects("test@example.com").returning(Future.successful(Some(user)))

        val result = userService.login(loginRequest).futureValue

        result shouldBe Left(ErrorResponse("Invalid credentials"))
      }
    }

    "listing users" should {

      "successfully return a list of users" in {
        val users = Seq(User(UUID.randomUUID(), "test1@example.com", BCrypt.hashpw("password1", BCrypt.gensalt()), UserRole.User, LocalDateTime.now()))
        val userInfos = users.map(user => UserInfo(user.id, user.email, user.role, user.registeredAt))
        (mockRepo.list _).expects(100, 0).returning(Future.successful(users))

        val result = userService.listUsers(100, 0).futureValue

        result shouldBe Right(userInfos)
      }

      "fail when repository throws an exception" in {
        (mockRepo.list _).expects(100, 0).returning(Future.failed(new Exception("DB error")))

        val result = userService.listUsers(100, 0).futureValue

        result shouldBe Left(ErrorResponse("Failed to list users: DB error"))
      }
    }
  }
}
