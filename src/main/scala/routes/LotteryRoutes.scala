package routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Directive1, RequestContext, Route}
import models.LotteryStatus
import services.LotteryService
import utils.JsonSupport
import auth.{Authorizer, JwtAuth}
import models.dto.{CreateLotteryRequest, SubmitBallotsRequest}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class LotteryRoutes(
 service: LotteryService,
 override val jwtAuth: JwtAuth
)(implicit ec: ExecutionContext) extends JsonSupport with Authorizer {

  def lotteryStatusParam(name: String): Directive1[Option[LotteryStatus]] = {
    parameter(name.as[String].?).map { p =>
      p.flatMap(LotteryStatus.fromString)
    }
  }

  def localDateParam(name: String): Directive1[Option[LocalDate]] = {
    parameter(name.as[String].?).map(_.flatMap(dateStr => Try(LocalDate.parse(dateStr)).toOption))
  }

  def extractToken: Directive1[String] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(token) => provide(token)
      case None => reject(AuthorizationFailedRejection)
    }
  }

  val routes: Route =
    concat(
      path("lotteries") {
        post {
          authorizeRoles(Set("admin")) {
            entity(as[CreateLotteryRequest]) { lottery =>
              complete(service.addLottery(lottery))
            }
          }
        }
      },
      path("lotteries") {
        get {
          authorizeRoles(Set("admin", "user")) {
            lotteryStatusParam("status") { statusOpt =>
              localDateParam("drawDate") { drawDateOpt =>
                complete(service.listLotteries(statusOpt, drawDateOpt))
              }
            }
          }
        }
      },
      path("lotteries" / JavaUUID) { id =>
        get {
          authorizeRoles(Set("admin", "user")) {
            complete(service.getLotteryById(id))
          }
        }
      },
      path("lotteries" / "ballots") {
        post {
          authorizeRoles(Set("user")) {
            extractToken { token =>
              onSuccess(Future.successful(jwtAuth.decodeToken(token))) {
                case Success((userId, _)) =>
                  entity(as[SubmitBallotsRequest]) { request =>
                    complete(service.addBallotsToLottery(request.lotteryId, userId, request.ballotsNumber))
                  }
                case Failure(_) => complete(StatusCodes.Unauthorized, "Invalid token")
              }
            }
          }
        }
      },
      path("lotteries" / JavaUUID / "ballots") { lotteryId =>
        parameters("limit".as[Int].?, "offset".as[Int].?) { (limitOpt, offsetOpt) =>
          val limit = limitOpt.getOrElse(100)
          val offset = offsetOpt.getOrElse(0)
          authorizeRoles(Set("user")) {
            extractToken { token =>
              onSuccess(Future.successful(jwtAuth.decodeToken(token))) {
                case Success((userId, _)) =>
                  complete(service.listBallots(lotteryId, userId, limit, offset))
                case Failure(_) => complete(StatusCodes.Unauthorized, "Invalid token")
              }
            }
          }
        }
      }
    )
}
