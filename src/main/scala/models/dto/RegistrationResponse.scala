package models.dto

import io.circe.generic.JsonCodec

@JsonCodec
case class RegistrationResponse(email: String)