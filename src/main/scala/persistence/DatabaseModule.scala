package persistence

import slick.jdbc.{JdbcProfile, SQLiteProfile}

trait DatabaseModule {
  val profile: JdbcProfile = SQLiteProfile
}
