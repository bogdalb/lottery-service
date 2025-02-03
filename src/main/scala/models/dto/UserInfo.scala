package models.dto

import io.circe.generic.JsonCodec
import models.UserRole

import java.time.LocalDateTime
import java.util.UUID

@JsonCodec
case class UserInfo(id: UUID, email: String, role: UserRole, registeredAt: LocalDateTime)

