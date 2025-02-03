package persistence.tables

import models.Ballot
import slick.jdbc.JdbcProfile
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BallotTable(val profile: JdbcProfile) extends TableUtils {
  import profile.api._

  class Ballots(tag: Tag) extends Table[Ballot](tag, "ballots") {

    def id = column[UUID]("id", O.PrimaryKey)

    def lotteryId = column[UUID]("lottery_id")

    def userId = column[UUID]("user_id")

    def obtainedAt = column[LocalDateTime]("obtained_at")

    def * = (id, lotteryId, userId, obtainedAt) <> (Ballot.tupled, Ballot.unapply)
  }

  val ballots = TableQuery[Ballots]

  def createTableIfNotExists(db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      _ <- createTableIfNotExists(ballots, db)
      _ <- createIndexIfNotExists(db, "idx_ballots_lottery_user", "CREATE INDEX idx_ballots_lottery_user ON ballots (lottery_id, user_id)")
      _ <- createIndexIfNotExists(db, "idx_ballots_lottery_id", "CREATE INDEX idx_ballots_lottery_id ON ballots (lottery_id)")
      _ <- createIndexIfNotExists(db, "idx_ballots_obtained_at", "CREATE INDEX idx_ballots_obtained_at ON ballots (obtained_at)")
    } yield ()
  }
}
