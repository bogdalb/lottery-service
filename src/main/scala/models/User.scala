package models

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class User(id: UUID, email: String, passwordHash: String, role: UserRole)

