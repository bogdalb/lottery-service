package services

import com.github.benmanes.caffeine.cache.Cache
import models.dto.{CreateLotteryRequest, ErrorResponse, CreateLotteryResponse}
import models.{Ballot, Lottery, LotteryStatus}
import persistence.repositories.{BallotRepository, LotteryRepository}
import utils.UserLockSupport
import utils.UserLockSupport.createUserLockCache

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import scala.concurrent.{ExecutionContext, Future}

class LotteryService(
  lotteryRepo: LotteryRepository,
  ballotRepo: BallotRepository
  )(implicit ec: ExecutionContext) extends UserLockSupport {

  private val UserBallotLimit = 100
  override val userLocks: Cache[UUID, ReentrantLock] = createUserLockCache(10)

  def addLottery(lotteryRequest: CreateLotteryRequest): Future[Either[ErrorResponse, CreateLotteryResponse]] = {
    val lotteryToCreate = Lottery(UUID.randomUUID(), lotteryRequest.drawDate, LotteryStatus.Active, None)
    lotteryRepo.addLottery(lotteryToCreate).map {
      case 1 => Right(CreateLotteryResponse(lotteryToCreate.id))
      case _ => Left(ErrorResponse("Failed to add lottery"))
    }.recover {
      case _ => Left(ErrorResponse("Error occurred while adding lottery"))
    }
  }

  def getLotteryById(id: UUID): Future[Either[ErrorResponse, Lottery]] = {
    lotteryRepo.getLotteryById(id).map {
      case Some(lottery) => Right(lottery)
      case None => Left(ErrorResponse("Lottery not found"))
    }.recover {
      case _ => Left(ErrorResponse("Error occurred while retrieving lottery"))
    }
  }

  def listLotteries(statusOpt: Option[LotteryStatus], drawDateOpt: Option[LocalDate]): Future[Either[ErrorResponse, Seq[Lottery]]] = {
    lotteryRepo.listLotteries(statusOpt, drawDateOpt).map(Right(_))
      .recover {
      case _ => Left(ErrorResponse("Error occurred while listing lotteries"))
    }
  }

  def addBallotsToLottery(lotteryId: UUID, userId: UUID, amount: Int): Future[Either[String, Seq[UUID]]] = {
    val obtainedAt = LocalDateTime.now()

    if (amount <= 0) {
      Future.successful(Left("Ballots amount should be positive"))
    } else {
      lotteryRepo.getLotteryById(lotteryId).flatMap {
        case Some(lottery) if obtainedAt.isBefore(lottery.drawDate.atStartOfDay()) =>
          withUserLock(userId) {
            ballotRepo.count(lotteryId, Some(userId)).flatMap { ballotsInLottery =>
              if ((ballotsInLottery + amount) > UserBallotLimit) {
                Future.successful(Left(s"Limit violation. User can submit at most ${UserBallotLimit - ballotsInLottery} ballots for this lottery"))
              } else {
                val ballotIds = Seq.fill(amount)(UUID.randomUUID())
                val ballots = ballotIds.map(Ballot(_, lotteryId, userId, obtainedAt))
                ballotRepo.add(ballots).map(_ => Right(ballotIds))
              }
            }
          }

        case Some(_) =>
          Future.successful(Left("Cannot add ballots to a lottery that has already ended"))
        case None =>
          Future.successful(Left(s"Cannot find active lottery with id $lotteryId"))
      }.recover {
        case _ => Left("Error occurred while adding ballots to lottery")
      }
    }
  }

  def updateLotteryStatus(lotteryId: UUID, status: LotteryStatus): Future[Either[ErrorResponse, Unit]] = {
    lotteryRepo.updateLotteryStatus(lotteryId, status, None).map {_ => Right()
    }.recover {
      case _ => Left(ErrorResponse("Error occurred while retrieving lottery"))
    }
  }

  def calculateLotteryResult(lotteryId: UUID): Future[Either[ErrorResponse, Lottery]] = {
    ballotRepo.count(lotteryId, None).flatMap {
      case ballotsAmount if ballotsAmount > 0 =>
        processLotteryWithBallots(lotteryId)
      case _ =>
        closeLotteryWithoutBallots(lotteryId)
    }.recover {
      case _ => Left(ErrorResponse("Error occurred while calculating lottery result"))
    }
  }

  def listBallots(lotteryId: UUID, userId: UUID, limit: Int, offset: Int): Future[Either[ErrorResponse, Seq[Ballot]]] = {
    if (limit <= 0 || offset < 0) {
      Future.successful(Left(ErrorResponse("Limit must be positive and offset must be non-negative")))
    } else {
      ballotRepo.list(lotteryId, userId, limit, offset).map(Right(_)).recover {
        case _ => Left(ErrorResponse("Error occurred while listing ballots"))
      }
    }
  }

  private def processLotteryWithBallots(lotteryId: UUID): Future[Either[ErrorResponse, Lottery]] = {
    lotteryRepo.getLotteryById(lotteryId).flatMap {
      case Some(lottery) if lottery.status == LotteryStatus.Closing =>
        selectWinnerAndUpdateStatus(lotteryId, lottery)
      case Some(_) =>
        Future.successful(Left(ErrorResponse("Lottery must be in 'Closing' status to calculate the result")))
      case None =>
        Future.successful(Left(ErrorResponse("Lottery not found")))
    }
  }

  private def selectWinnerAndUpdateStatus(lotteryId: UUID, lottery: Lottery): Future[Either[ErrorResponse, Lottery]] = {
    ballotRepo.getRandom(lotteryId).flatMap {
      case Some(winnerBallot) =>
        lotteryRepo.updateLotteryStatus(lotteryId, LotteryStatus.Closed, Some(winnerBallot.id)).map {
          case 1 =>
            Right(lottery.copy(status = LotteryStatus.Closed, winnerBallot = Some(winnerBallot.id)))
          case _ =>
            Left(ErrorResponse("Failed to update lottery status"))
        }
      case None =>
        Future.successful(Left(ErrorResponse("No winner found for the lottery")))
    }
  }

  private def closeLotteryWithoutBallots(lotteryId: UUID): Future[Either[ErrorResponse, Lottery]] = {
    lotteryRepo.updateLotteryStatus(lotteryId, LotteryStatus.Closed, None).map {
      case 1 => Left(ErrorResponse("Closed due to absence of ballots"))
      case _ => Left(ErrorResponse("Failed to update lottery status"))
    }
  }
}
