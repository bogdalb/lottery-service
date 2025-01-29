package models.dto

import io.circe.generic.JsonCodec

@JsonCodec
case class LoginResponse(token: String)
