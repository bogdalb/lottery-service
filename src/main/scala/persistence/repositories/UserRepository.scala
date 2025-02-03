package persistence.repositories


import models.User
import persistence.tables.UserTable

import scala.concurrent.Future

trait UserRepository {

  def add(user: User): Future[Int]

  def getByEmail(email: String): Future[Option[User]]

  def list(limit: Int, offset: Int): Future[Seq[User]]
}

class SlickUserRepository(db: slick.jdbc.SQLiteProfile.backend.Database, tables: UserTable) extends UserRepository {
  import tables.profile.api._

  override def add(user: User): Future[Int] = db.run(tables.users += user)

  override def getByEmail(email: String): Future[Option[User]] = db.run(tables.users.filter(_.email === email).result.headOption)

  override def list(limit: Int, offset: Int): Future[Seq[User]] = {
    val query = tables.users
      .sortBy(_.registeredAt.asc)
      .drop(offset)
      .take(limit)

    db.run(query.result)
  }
}



