package persistence.repositories

import models.Ballot
import persistence.tables.BallotTable

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BallotRepository {

  def add(ballots: Seq[Ballot]): Future[Int]

  def list(userId: UUID, lotteryId: UUID, limit: Int, offset: Int): Future[Seq[Ballot]]

  def count(lotteryId: UUID, userIdOpt: Option[UUID]): Future[Int]

  def getRandom(lotteryId: UUID): Future[Option[Ballot]]
}

class SlickBallotRepository(
  db: slick.jdbc.SQLiteProfile.backend.Database,
                             tables: BallotTable
                           ) extends BallotRepository {

  import tables.profile.api._

  override def add(ballots: Seq[Ballot]): Future[Int] = {
    val action = tables.ballots ++= ballots
    db.run(action).map(_.getOrElse(0))
  }

  override def list(userId: UUID, lotteryId: UUID, limit: Int, offset: Int): Future[Seq[Ballot]] = {
    val query = tables.ballots
      .filter(row => row.userId === userId && row.lotteryId === lotteryId)
      .drop(offset)
      .take(limit)

    db.run(query.result)
  }

  override def count(lotteryId: UUID, userIdOpt: Option[UUID]): Future[Int] = {
    val baseQuery = tables.ballots.filter(_.lotteryId === lotteryId)
    val query = userIdOpt match {
      case Some(userId) => baseQuery.filter(_.userId === userId)
      case None         => baseQuery
    }
    db.run(query.size.result)
  }

  override def getRandom(lotteryId: UUID): Future[Option[Ballot]] = {
    val query = tables.ballots
      .filter(_.lotteryId === lotteryId)
      .sortBy(_ => slick.lifted.SimpleLiteral[Boolean]("RANDOM()"))
      .take(1)

    db.run(query.result.headOption)
  }

}
