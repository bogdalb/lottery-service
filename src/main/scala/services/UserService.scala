package services

import auth.JwtAuth
import models.dto.{ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegistrationResponse}

import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt
import persistence.repositories.UserRepository
import models.{User, UserRole}

class UserService(repo: UserRepository, jwtAuth: JwtAuth)(implicit ec: ExecutionContext) {

  def register(request: RegisterRequest, role: UserRole): Future[Either[ErrorResponse, RegistrationResponse]] = {
    val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
    val user = User(
      id = UUID.randomUUID(),
      email = request.email,
      passwordHash = hashedPassword,
      role = role
    )

    repo.add(user).map { _ =>
      Right(RegistrationResponse(user.email))
    }.recover {
      case ex: Exception =>
        Left(ErrorResponse(s"Registration failed: ${ex.getMessage}"))
    }
  }

  def login(request: LoginRequest): Future[Either[ErrorResponse, LoginResponse]] = {
    repo.getByEmail(request.email).flatMap {
      case Some(user) if BCrypt.checkpw(request.password, user.passwordHash) =>
        val token = jwtAuth.createToken(user.id, user.role)
        Future.successful(Right(LoginResponse(token)))
      case _ => Future.successful(Left(ErrorResponse("Invalid credentials")))
    }
  }

  def listUsers(limit: Int, offset: Int): Future[Either[ErrorResponse, Seq[User]] ] = {
    repo.list(limit, offset).map { users =>
      Right(users)
    }.recover {
      case ex: Exception =>
        Left(ErrorResponse(s"Failed to list users: ${ex.getMessage}"))
    }
  }
}
