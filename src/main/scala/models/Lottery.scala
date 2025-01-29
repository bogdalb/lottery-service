package models

import java.util.UUID
import io.circe.generic.JsonCodec

import java.time.{LocalDate, ZonedDateTime}

@JsonCodec
case class Lottery(id: UUID, drawDate: LocalDate, status: LotteryStatus, winnerBallot: Option[UUID])
