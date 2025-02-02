package models.dto

import io.circe.generic.JsonCodec

import java.time.LocalDate

@JsonCodec
case class CreateLotteryRequest(drawDate: LocalDate)