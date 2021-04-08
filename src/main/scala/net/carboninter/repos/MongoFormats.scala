package net.carboninter.repos

import play.api.libs.json.{JsObject, JsPath, JsValue, Json, Reads, Writes}

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import reactivemongo.api.bson.BSONDateTime

trait MongoFormats {

//  implicit class MongoOffsetDateTime(offsetDateTime: OffsetDateTime) {
//    def toMongo = BSONDateTime(offsetDateTime.toInstant.toEpochMilli)
//  }

  implicit class MongoOffsetDateTime(offsetDateTime: OffsetDateTime) {
    def toMongo: JsObject = Json.obj("$date" -> offsetDateTime.toInstant.toEpochMilli)
    def toBSON = BSONDateTime(offsetDateTime.toInstant.toEpochMilli)
  }

  implicit val offsetDateTimeRead: Reads[OffsetDateTime] =
    (JsPath \ "$date").read[Long].map { millis =>
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
    }

  implicit val offsetDateTimeWrite: Writes[OffsetDateTime] = new Writes[OffsetDateTime] {
    def writes(offsetDateTime: OffsetDateTime): JsValue = offsetDateTime.toMongo
  }
}
