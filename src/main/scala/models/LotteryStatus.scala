package models

import io.circe.{Decoder, Encoder}
import io.circe.generic.JsonCodec


sealed trait LotteryStatus

object LotteryStatus {
  case object Active extends LotteryStatus
  case object Closing extends LotteryStatus
  case object Closed extends LotteryStatus

  def fromString(status: String): Option[LotteryStatus] = status.toLowerCase match {
    case "active" => Some(Active)
    case "closing"  => Some(Closing)
    case "closed"  => Some(Closed)
    case _       => None
  }

  def toString(status: LotteryStatus): String = status.toString

  implicit val lotteryStatusEncoder: Encoder[LotteryStatus] = Encoder.encodeString.contramap(_.toString)
  implicit val lotteryStatusDecoder: Decoder[LotteryStatus] = Decoder.decodeString.emap {
    fromString(_).fold[Either[String, LotteryStatus]](Left(s"Invalid LotteryStatus"))(Right(_))
  }
}
