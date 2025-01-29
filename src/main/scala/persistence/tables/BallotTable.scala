package persistence.tables

import models.Ballot
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class BallotTable(val profile: JdbcProfile) {
  import profile.api._

  class Ballots(tag: Tag) extends Table[Ballot](tag, "ballots") {

    def id = column[UUID]("id", O.PrimaryKey)

    def lotteryId = column[UUID]("lottery_id")

    def userId = column[UUID]("user_id")

    def obtainedAt = column[LocalDateTime]("obtained_at")

    def * = (id, lotteryId, userId, obtainedAt) <> (Ballot.tupled, Ballot.unapply)
  }

  val ballots = TableQuery[Ballots]

  def indexExists(db: JdbcProfile#Backend#Database, indexName: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val checkIndexQuery = sql"""
      SELECT 1
      FROM pg_indexes
      WHERE indexname = $indexName
    """.as[Int]

    db.run(checkIndexQuery.headOption).map(_.isDefined)
  }

  def createIndexIfNotExists(db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      lotteryUserIndexExists <- indexExists(db, "idx_lottery_user")
      lotteryIndexExists <- indexExists(db, "idx_lottery_id")
      _ <- {
        if (!lotteryUserIndexExists) {
          db.run(sql"CREATE INDEX idx_lottery_user ON ballots (lottery_id, user_id)".asUpdate)
        } else {
          Future.successful(())
        }
      }
      _ <- {
        if (!lotteryIndexExists) {
          db.run(sql"CREATE INDEX idx_lottery_id ON ballots (lottery_id)".asUpdate)
        } else {
          Future.successful(())
        }
      }
    } yield ()
  }

  def createTableIfNotExists(db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Unit] = {
    val setup = DBIO.seq(ballots.schema.createIfNotExists)
    db.run(setup).map(_ => ())
  }
}
