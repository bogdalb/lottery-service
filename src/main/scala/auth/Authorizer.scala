package auth

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.util.Success

trait Authorizer {
  def authorizeRoles(allowedRoles: Set[String])(route: => Route): Route = {
    optionalHeaderValueByName("Authorization") {
      case Some(authorizationHeader) =>
        if (authorizationHeader.startsWith("Bearer ")) {
          val token = authorizationHeader.stripPrefix("Bearer ").trim
          jwtAuth.decodeToken(token) match {
            case Success((_, role)) if allowedRoles.contains(role) =>
              route
            case _ => complete((403, "Forbidden: insufficient permissions"))
          }
        } else {
          complete((400, "Invalid Authorization header format"))
        }
      case None =>
        complete((401, "Unauthorized: missing token"))
    }

  }

  protected def jwtAuth: JwtAuth
}
