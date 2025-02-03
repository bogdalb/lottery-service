package persistence.tables

import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

trait TableUtils {
  val profile: JdbcProfile
  import profile.api._

  private def indexExists(db: JdbcProfile#Backend#Database, indexName: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    val checkIndexQuery = sql"""
        SELECT name
          FROM sqlite_master
          WHERE type = 'index' AND name = $indexName;
        """.as[String]
    db.run(checkIndexQuery.headOption).map(_.isDefined)
  }

  def createIndexIfNotExists(db: JdbcProfile#Backend#Database, indexName: String, createIndexSql: String)(implicit ec: ExecutionContext): Future[Unit] = {
    indexExists(db, indexName).flatMap {
      case false =>
        db.run(sqlu"#$createIndexSql").map(_ => ())
      case true =>
        Future.successful(())
    }
  }

  def createTableIfNotExists[T <: Table[_]](tableQuery: TableQuery[T], db: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext): Future[Unit] = {
    val createTableAction = tableQuery.schema.createIfNotExists
    db.run(createTableAction).map(_ => ())
  }
}
