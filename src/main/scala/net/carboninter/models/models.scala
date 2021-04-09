package net.carboninter.models

import play.api.libs.json.{JsObject, JsPath, JsValue, Json, Reads, Writes}

import java.time.{Instant, OffsetDateTime, ZoneOffset}

//case class ExtendedTweet(full_text: String)
//object ExtendedTweet {
//  implicit val extendedTweetHandler: BSONDocumentHandler[ExtendedTweet] = Macros.handler[ExtendedTweet]
//}
//
//case class Tweet(
//  created_at: String,
//  id_str: String
//)
//
//object Tweet {
//
//  implicit val instantReader: BSONReader[Instant] = BSONReader.collect[Instant] {
//    case BSONString(str) => Instant.ofEpochMilli(str.toLong)
//  }
//
//  implicit val tweetHandler: BSONDocumentHandler[Tweet] = Macros.handler[Tweet]
//}

case class DissectedTweet(tweet: JsObject, retweetedTweet: Option[JsObject], quotedTweet: Option[JsObject]) {
  def retweetsAnotherTweet = retweetedTweet.isDefined
  def quotesAnotherTweet = quotedTweet.isDefined
}

case class LiveRunner(id: String, name: String, date: String, time24: String)
object LiveRunner {
  implicit val format = Json.format[LiveRunner]
}

case class TmpId(_id: String)
object TmpId {
  implicit val format = Json.format[TmpId]
}

case class LiveEvent(track: String, marketFirstAppearance: OffsetDateTime, marketStartTime: OffsetDateTime, liveRunners: List[LiveRunner])
object LiveEvent {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val offsetDateTimeRead: Reads[OffsetDateTime] =
    (JsPath \ "$date" \ "$numberLong").read[String].map { millis =>
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis.toLong), ZoneOffset.UTC)
    }


  implicit val reads  =
    ((__ \ "_id" \ "_id").read[String]
    ~ (__ \ "marketFirstAppearance").read[OffsetDateTime]
    ~ (__ \ "marketStartTime").read[OffsetDateTime]
    ~ (__ \ "liveRunners").read[List[LiveRunner]]
    )(LiveEvent.apply(_, _, _, _))
}