package persistence.tables

import models.Ballot
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BallotTable(val profile: JdbcProfile) {
  import profile.api._

  class Ballots(tag: Tag) extends Table[Ballot](tag, "ballots") {

    def id = column[UUID]("id", O.PrimaryKey)

    def lotteryId = column[UUID]("lottery_id")

    def userId = column[UUID]("user_id")

    def obtainedAt = column[LocalDateTime]("obtained_at")

    def lotteryUserIndex = index("idx_lottery_user", (lotteryId, userId), unique = false)

    def lotteryIndex = index("idx_lottery_id", lotteryId, unique = false)

    def * = (id, lotteryId, userId, obtainedAt) <> (Ballot.tupled, Ballot.unapply)

  }

  val ballots = TableQuery[Ballots]

  def createTableIfNotExists(db: JdbcProfile#Backend#Database): Future[Unit] = {
    val setup = DBIO.seq(ballots.schema.createIfNotExists)
    db.run(setup).map(_ => ())
  }
}


