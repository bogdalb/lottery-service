package services

import models.dto.{ErrorResponse, SubmitBallotsRequest, СreateLotteryRequest, СreateLotteryResponse}
import models.{Ballot, Lottery, LotteryStatus}
import persistence.repositories.{BallotRepository, LotteryRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LotteryServiceTest extends AnyWordSpec with Matchers with ScalaFutures with MockFactory {

  val lotteryRepo: LotteryRepository = mock[LotteryRepository]
  val ballotRepo: BallotRepository = mock[BallotRepository]
  val lotteryService = new LotteryService(lotteryRepo, ballotRepo)

  val lotteryId = UUID.randomUUID()
  val userId = UUID.randomUUID()
  val ballotId = UUID.randomUUID()
  val lottery = Lottery(lotteryId, LocalDate.now().plusDays(3), LotteryStatus.Active, None)
  val createLotteryRequest = СreateLotteryRequest(lottery.drawDate)
  val createLotteryResponse = СreateLotteryResponse(lottery.id)
  val ballot = Ballot(ballotId, lotteryId, userId, LocalDateTime.now())

  "LotteryService" when {

    "adding a lottery" should {
      "successfully add a lottery" in {
        (lotteryRepo.addLottery _).expects(*).returning(Future.successful(1))

        whenReady(lotteryService.addLottery(createLotteryRequest)) { result =>
          result shouldBe a[Right[_, _]]
        }
      }

      "return error when adding a lottery fails" in {
        (lotteryRepo.addLottery _).expects(*).returning(Future.successful(0))

        whenReady(lotteryService.addLottery(createLotteryRequest)) { result =>
          result shouldBe Left(ErrorResponse("Failed to add lottery"))
        }
      }

      "return error if adding lottery fails with exception" in {
        (lotteryRepo.addLottery _).expects(*).returning(Future.failed(new Exception("DB error")))

        whenReady(lotteryService.addLottery(createLotteryRequest)) { result =>
          result shouldBe Left(ErrorResponse("Error occurred while adding lottery"))
        }
      }
    }


    "getting a lottery by id" should {
      "successfully get a lottery by id" in {
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(lottery)))

        whenReady(lotteryService.getLotteryById(lotteryId)) { result =>
          result shouldBe Right(lottery)
        }
      }

      "return error if lottery not found by id" in {
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(None))

        whenReady(lotteryService.getLotteryById(lotteryId)) { result =>
          result shouldBe Left(ErrorResponse("Lottery not found"))
        }
      }

      "return error if getting lottery by id fails with exception" in {
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.failed(new Exception("DB error")))

        whenReady(lotteryService.getLotteryById(lotteryId)) { result =>
          result shouldBe Left(ErrorResponse("Error occurred while retrieving lottery"))
        }
      }
    }

    "listing lotteries" should {
      "successfully list lotteries" in {
        val lotteries = Seq(lottery)
        (lotteryRepo.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(lotteries))

        whenReady(lotteryService.listLotteries(Some(LotteryStatus.Active), None)) { result =>
          result shouldBe Right(lotteries)
        }
      }

      "return error if no lotteries found" in {
        (lotteryRepo.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.successful(Seq.empty))

        whenReady(lotteryService.listLotteries(Some(LotteryStatus.Active), None)) { result =>
          result shouldBe Left(ErrorResponse("No lotteries found"))
        }
      }

      "return error if listing lotteries fails with exception" in {
        (lotteryRepo.listLotteries _).expects(Some(LotteryStatus.Active), None).returning(Future.failed(new Exception("DB error")))

        whenReady(lotteryService.listLotteries(Some(LotteryStatus.Active), None)) { result =>
          result shouldBe Left(ErrorResponse("Error occurred while listing lotteries"))
        }
      }
    }

    "adding ballots to a lottery" should {
      "successfully add ballots to an active lottery" in {
        val amount = 5
        val existingBallots = 10

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(lottery)))
        (ballotRepo.count _).expects(lotteryId, Some(userId)).returning(Future.successful(existingBallots))
        (ballotRepo.add _).expects(*).returning(Future.successful(5))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe a[Right[_, _]]
          result.map(_.size) shouldBe Right(amount)
        }
      }

      "fail if the user exceeds the ballot limit" in {
        val amount = 95
        val existingBallots = 10

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(lottery)))
        (ballotRepo.count _).expects(lotteryId, Some(userId)).returning(Future.successful(existingBallots))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe Left(s"Limit violation. User can submit at most ${100 - existingBallots} ballots for this lottery")
        }
      }

      "fail if the lottery has already ended" in {
        val pastLottery = lottery.copy(drawDate = LocalDate.now().minusDays(1))

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(pastLottery)))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 5)) { result =>
          result shouldBe Left("Cannot add ballots to a lottery that has already ended")
        }
      }

      "fail if the lottery is not found" in {
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(None))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 5)) { result =>
          result shouldBe Left(s"Cannot find active lottery with id $lotteryId")
        }
      }

      "fail if the amount of ballots is non-positive" in {
        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 0)) { result =>
          result shouldBe Left("Ballots amount should be positive")
        }
      }

      "fail if an exception occurs" in {
        val amount = 5

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.failed(new Exception("DB error")))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe a[Left[_, _]]
        }
      }
    }

    "adding ballots to a lottery via route" should {

      "successfully add ballots to a lottery" in {
        val amount = 5
        val existingBallots = 10

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(lottery)))
        (ballotRepo.count _).expects(lotteryId, Some(userId)).returning(Future.successful(existingBallots))
        (ballotRepo.add _).expects(*).returning(Future.successful(5))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe a[Right[_, _]]
          result.map(_.size) shouldBe Right(amount)
        }
      }

      "fail if the user exceeds the ballot limit via route" in {
        val amount = 95
        val existingBallots = 10

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(lottery)))
        (ballotRepo.count _).expects(lotteryId, Some(userId)).returning(Future.successful(existingBallots))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe Left(s"Limit violation. User can submit at most ${100 - existingBallots} ballots for this lottery")
        }
      }

      "fail if the lottery has already ended via route" in {
        val pastLottery = lottery.copy(drawDate = LocalDate.now().minusDays(1))

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(pastLottery)))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 5)) { result =>
          result shouldBe Left("Cannot add ballots to a lottery that has already ended")
        }
      }

      "fail if the lottery is not found via route" in {
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(None))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 5)) { result =>
          result shouldBe Left(s"Cannot find active lottery with id $lotteryId")
        }
      }

      "fail if the amount of ballots is non-positive via route" in {
        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, 0)) { result =>
          result shouldBe Left("Ballots amount should be positive")
        }
      }

      "fail if an exception occurs while adding ballots via route" in {
        val amount = 5

        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.failed(new Exception("DB error")))

        whenReady(lotteryService.addBallotsToLottery(lotteryId, userId, amount)) { result =>
          result shouldBe a[Left[_, _]]
        }
      }
    }

    "picking the winner of a lottery" should {

      "successfully picking the winner of a lottery and update the lottery status" in {

        val closingLottery = lottery.copy(status = LotteryStatus.Closing)

        (ballotRepo.count _).expects(lottery.id, None).returning(Future.successful(5))
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(closingLottery)))
        (ballotRepo.getRandom _).expects(lotteryId).returning(Future.successful(Some(ballot)))
        (lotteryRepo.updateLotteryStatus _).expects(lotteryId, LotteryStatus.Closed, Some(ballot.id)).returning(Future.successful(1))

        whenReady(lotteryService.calculateLotteryResult(lotteryId)) { result =>
          result shouldBe Right(lottery.copy(status = LotteryStatus.Closed, winnerBallot = Some(ballot.id)))
        }
      }

      "fail if cannot update the lottery status" in {

        val closingLottery = lottery.copy(status = LotteryStatus.Closing)

        (ballotRepo.count _).expects(lottery.id, None).returning(Future.successful(5))
        (lotteryRepo.getLotteryById _).expects(lotteryId).returning(Future.successful(Some(closingLottery)))
        (ballotRepo.getRandom _).expects(lotteryId).returning(Future.successful(Some(ballot)))
        (lotteryRepo.updateLotteryStatus _).expects(lotteryId, LotteryStatus.Closed, Some(ballot.id)).returning(Future.successful(0))

        whenReady(lotteryService.calculateLotteryResult(lotteryId)) { result =>
          result shouldBe Left(ErrorResponse("Failed to update lottery status"))
        }
      }

      "fail and close lottery due Closed due to absence of ballots" in {

        (ballotRepo.count _).expects(lottery.id, None).returning(Future.successful(0))
        (lotteryRepo.updateLotteryStatus _).expects(lotteryId, LotteryStatus.Closed, None).returning(Future.successful(1))

        whenReady(lotteryService.calculateLotteryResult(lotteryId)) { result =>
          result shouldBe Left(ErrorResponse("Closed due to absence of ballots"))
        }
      }
    }
  }
}
