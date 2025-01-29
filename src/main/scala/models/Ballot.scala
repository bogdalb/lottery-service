package models

import io.circe.generic.JsonCodec

import java.time.LocalDateTime
import java.util.UUID

@JsonCodec
case class Ballot(id: UUID, lotteryId: UUID, userId: UUID, obtainedAt: LocalDateTime)

