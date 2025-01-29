package models.dto

import io.circe.generic.JsonCodec

@JsonCodec
case class LoginRequest(email: String, password: String)