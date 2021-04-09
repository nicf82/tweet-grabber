package net.carboninter.models

import java.time.{Instant, OffsetDateTime, ZoneOffset}

case class StubTweet(
  id_str: String,
  text: String,
  time: OffsetDateTime
) {
  def noNewLines = copy(text = text.replaceAll("\n", " "))
}

object StubTweet {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val reads: Reads[StubTweet] =
    (
      (__ \ "id_str").read[String] and
      (__ \ "extended_tweet" \ "text").read[String].orElse((__ \ "text").read[String]) and
      (__ \ "timestamp_ms").read[String].map(ms => Instant.ofEpochMilli(ms.toLong).atOffset(ZoneOffset.UTC))
    )(StubTweet.apply _)

}