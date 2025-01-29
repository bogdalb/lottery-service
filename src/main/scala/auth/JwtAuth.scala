package auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import models.UserRole

import java.util.UUID
import scala.util.Try
import scala.util.Failure
import scala.util.Success

trait JwtAuth {
  def createToken(userId: UUID, role: UserRole): String
  def decodeToken(token: String): Try[(UUID, String)]
}

case class JwtAuthImpl(config: JwtAuthConfiguration) extends JwtAuth {

  private val algorithm = Algorithm.HMAC256(config.secretKey)

  override def createToken(userId: UUID, role: UserRole): String = {
    JWT.create()
      .withSubject(userId.toString)
      .withClaim("role", UserRole.toString(role))
      .withIssuedAt(new java.util.Date())
      .withExpiresAt(new java.util.Date(System.currentTimeMillis() + config.expirationTimeInSeconds * 1000))
      .sign(algorithm)
  }

  override def decodeToken(token: String): Try[(UUID, String)] = {
    Try {
      val decodedJWT = JWT.require(algorithm)
        .build()
        .verify(token)

      val userId = UUID.fromString(decodedJWT.getSubject)
      val role = decodedJWT.getClaim("role").asString()

      (userId, role)
    } match {
      case Success(value) => Success(value)
      case Failure(exception: JWTVerificationException) =>
        Failure(new IllegalArgumentException("Invalid token", exception))
      case Failure(exception) =>
        Failure(exception)
    }
  }
}
