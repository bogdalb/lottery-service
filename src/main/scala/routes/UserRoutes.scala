package routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import models.UserRole
import services.UserService
import auth.{JwtAuth, Authorizer}
import models.dto.{LoginRequest, RegisterRequest}
import utils.JsonSupport

import scala.concurrent.ExecutionContext

class UserRoutes(
  service: UserService,
  override val jwtAuth: JwtAuth
  )(implicit ec: ExecutionContext) extends JsonSupport with Authorizer {

  val routes: Route =
    concat(
      path("admin" / "register") {
        post {
          entity(as[RegisterRequest]) { request =>
            complete(service.register(request, UserRole.Admin))
          }
        }
      },
      path("user" / "register") {
        post {
          entity(as[RegisterRequest]) { request =>
            complete(service.register(request, UserRole.User))
          }
        }
      },
      path("user" / "login") {
        post {
          entity(as[LoginRequest]) { request =>
            complete(service.login(request))
          }
        }
      },
      pathPrefix("users") {
        get {
          parameters("limit".as[Int].?, "offset".as[Int].?) { (limitOpt, offsetOpt) =>
            val limit = limitOpt.getOrElse(100)
            val offset = offsetOpt.getOrElse(0)

            authorizeRoles(Set("admin")) {
              complete(service.listUsers(limit, offset))
            }
          }
        }
      }
    )
}
