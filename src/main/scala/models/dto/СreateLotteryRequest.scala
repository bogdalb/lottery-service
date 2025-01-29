package models.dto

import io.circe.generic.JsonCodec

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

@JsonCodec
case class СreateLotteryRequest(drawDate: LocalDate)