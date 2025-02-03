package models

import io.circe.generic.JsonCodec
import io.scalaland.chimney.dsl.TransformationOps
import models.dto.UserInfo

import java.time.LocalDateTime
import java.util.UUID

@JsonCodec
case class User(id: UUID, email: String, passwordHash: String, role: UserRole, registeredAt: LocalDateTime)

object User {
  def toUserInfo(user: User): UserInfo = {
    user.into[UserInfo].transform
  }
}