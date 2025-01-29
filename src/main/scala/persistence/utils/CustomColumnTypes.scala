package persistence.utils

import models.{LotteryStatus, UserRole}

object CustomColumnTypes {
  import slick.jdbc.PostgresProfile.api._

  implicit val lotteryStatusMapper: BaseColumnType[LotteryStatus] = MappedColumnType.base[LotteryStatus, String](
    LotteryStatus.toString,
    LotteryStatus.fromString(_).getOrElse(throw new IllegalArgumentException("Invalid status"))
  )

  implicit val userRoleMapper: BaseColumnType[UserRole] = MappedColumnType.base[UserRole, String](
    UserRole.toString,
    UserRole.fromString(_).getOrElse(throw new IllegalArgumentException("Invalid role"))
  )
}