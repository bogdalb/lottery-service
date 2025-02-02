package persistence.tables

import slick.jdbc.JdbcProfile
import models.{User, UserRole}

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UserTable(val profile: JdbcProfile) {

  import profile.api._
  import persistence.utils.CustomColumnTypes._

  class Users(tag: Tag) extends Table[User](tag, "users") {

    def id = column[UUID]("id", O.PrimaryKey)

    def email = column[String]("email", O.Unique)

    def passwordHash = column[String]("password_hash")

    def role = column[UserRole]("role")

    def registeredAt = column[LocalDateTime]("registered_at")

    def * = (id, email, passwordHash, role, registeredAt) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  def createTableIfNotExists(db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Unit] = {
    val setup = DBIO.seq(users.schema.createIfNotExists)
    db.run(setup).map(_ => ())
  }
}
