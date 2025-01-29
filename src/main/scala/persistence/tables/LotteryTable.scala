package persistence.tables

import models.{Lottery, LotteryStatus}
import slick.jdbc.JdbcProfile

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LotteryTable(val profile: JdbcProfile) {
  import profile.api._
  import persistence.utils.CustomColumnTypes._

  class Lotteries(tag: Tag) extends Table[Lottery](tag, "lotteries") {
    def id = column[UUID]("id", O.PrimaryKey)
    def drawDate = column[LocalDate]("drawDate")
    def status = column[LotteryStatus]("status")
    def winnerBallot = column[Option[UUID]]("winner_ballot")

    def * = (id, drawDate, status, winnerBallot) <> (Lottery.tupled, Lottery.unapply)
  }

  val lotteries = TableQuery[Lotteries]

  def createTableIfNotExists(db: JdbcProfile#Backend#Database): Future[Unit] = {
    val setup = DBIO.seq(lotteries.schema.createIfNotExists)
    db.run(setup).map(_ => ())
  }
}


