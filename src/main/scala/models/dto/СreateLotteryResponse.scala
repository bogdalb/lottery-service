package models.dto

import io.circe.generic.JsonCodec

import java.util.UUID

@JsonCodec
case class СreateLotteryResponse(lotteryId: UUID)