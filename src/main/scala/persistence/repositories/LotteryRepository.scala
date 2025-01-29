package persistence.repositories

import models.{Lottery, LotteryStatus}
import persistence.tables.LotteryTable

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future

trait LotteryRepository {

  def addLottery(lottery: Lottery): Future[Int]

  def getLotteryById(id: UUID): Future[Option[Lottery]]

  def listLotteries(statusOpt: Option[LotteryStatus], drawDateOpt: Option[LocalDate]): Future[Seq[Lottery]]

  def updateLotteryStatus(id: UUID, newStatus: LotteryStatus, winnerBallotIdOpt: Option[UUID]): Future[Int]
}

class SlickLotteryRepository(db: slick.jdbc.SQLiteProfile.backend.Database, tables: LotteryTable) extends LotteryRepository {
  import tables.profile.api._
  import persistence.utils.CustomColumnTypes._


  override def addLottery(lottery: Lottery): Future[Int] =
    db.run(tables.lotteries += lottery)

  override def getLotteryById(id: UUID): Future[Option[Lottery]] =
    db.run(tables.lotteries.filter(_.id === id).result.headOption)

  override def listLotteries(statusOpt: Option[LotteryStatus], drawDateOpt: Option[LocalDate]): Future[Seq[Lottery]] =
    db.run {
      tables.lotteries
        .filterOpt(statusOpt)((lottery, status) => lottery.status === status)
        .filterOpt(drawDateOpt)((lottery, drawDate) => lottery.drawDate === drawDate)
        .result
    }

  def updateLotteryStatus(id: UUID, newStatus: LotteryStatus, winnerBallotIdOpt: Option[UUID]): Future[Int] = {
    val query = tables.lotteries
      .filter(_.id === id)
      .map(lottery => (lottery.status, lottery.winnerBallot))

    val updateAction = winnerBallotIdOpt match {
      case Some(winnerBallotId) =>
        query.update(newStatus, Some(winnerBallotId))
      case None =>
        query.update(newStatus, None)
    }

    db.run(updateAction)
  }

}


