package models

import io.circe.{Decoder, Encoder}

sealed trait UserRole

object UserRole {
  case object Admin extends UserRole
  case object User extends UserRole

  def fromString(role: String): Option[UserRole] = role.toLowerCase match {
    case "admin" => Some(Admin)
    case "user" => Some(User)
    case _ => None
  }

  def toString(role: UserRole): String = role match {
    case Admin => "Admin"
    case User => "User"
  }

  implicit val userRoleEncoder: Encoder[UserRole] = Encoder.encodeString.contramap(_.toString)
  implicit val userRoleDecoder: Decoder[UserRole] = Decoder.decodeString.emap {
    fromString(_).fold[Either[String, UserRole]](Left(s"Invalid UserRole"))(Right(_))
  }
}