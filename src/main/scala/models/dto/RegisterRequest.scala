package models.dto

import io.circe.generic.JsonCodec

@JsonCodec
case class RegisterRequest(email: String, password: String)