package utils

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.server.Directive1
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax._
import models.LotteryStatus
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directive1
import io.circe.Decoder
import scala.util.Try

import java.time.LocalDate

trait JsonSupport {
  implicit def circeJsonUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(MediaTypes.`application/json`).mapWithCharset { (data, charset) =>
      decode[A](data.decodeString(charset.nioCharset().name())).fold(throw _, identity)
    }

  implicit def circeJsonMarshaller[A: Encoder]: ToEntityMarshaller[A] =
    Marshaller.stringMarshaller(MediaTypes.`application/json`).compose(_.asJson.noSpaces)

}
