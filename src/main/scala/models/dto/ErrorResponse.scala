package models.dto

import io.circe.generic.JsonCodec

@JsonCodec
case class ErrorResponse(error: String)