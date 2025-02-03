package persistence.tables

import models.{User, UserRole}
import slick.jdbc.JdbcProfile
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class UserTable(val profile: JdbcProfile) extends TableUtils {
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
    for {
      _ <- createTableIfNotExists(users, db)
      _ <- createIndexIfNotExists(db, "idx_registered_at_user", "CREATE INDEX idx_registered_at_user ON users (registered_at)")
    } yield ()
  }
}
