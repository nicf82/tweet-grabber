package net.carboninter.models

import play.api.libs.json.{JsObject, JsResult, JsValue, Json, Reads}
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader, Macros}

import java.time.OffsetDateTime

case class Markers(batch: String)
object Markers {
  implicit val handler = Macros.handler[Markers]
}

case class Entry(
  _id: String,
  age: Option[Int],   //There are a few nulls, not sure why?
  country: String,
  createdAt: OffsetDateTime,
  date: String,
  firstAppearance: OffsetDateTime,
  jockey: Option[String],   //There are a few nulls, not sure why?
  marketStartTime: OffsetDateTime,
  name: String,
  runners: Int,
  time24: String,
  track: String,
  trainer: Option[String],   //There are a few nulls, not sure why?
  updatedAt: OffsetDateTime,
  markers: Option[Markers],
  tweets: Option[List[String]],
  bfSp: Option[String],
  bfWinReturn: Option[String],
  ewReturn: Option[String],
  indSp: Option[String],
  place: Option[String],
  spWinReturn: Option[String],
  winningDist: Option[String]
)

object Entry {

  //This is the reader, as we have more than 22 fields macros, along with apply/unapplys dont work :|
  implicit object EntryReader extends BSONDocumentReader[Entry] {
    def readDocument(bson: BSONDocument) = for {
      _id <- bson.getAsTry[String]("_id")
      age <- bson.getAsUnflattenedTry[Int]("age")
      country <- bson.getAsTry[String]("country")
      createdAt <- bson.getAsTry[OffsetDateTime]("createdAt")
      date <- bson.getAsTry[String]("date")
      firstAppearance <- bson.getAsTry[OffsetDateTime]("firstAppearance")
      jockey <- bson.getAsUnflattenedTry[String]("jockey")
      marketStartTime <- bson.getAsTry[OffsetDateTime]("marketStartTime")
      name <- bson.getAsTry[String]("name")
      runners <- bson.getAsTry[Int]("runners")
      time24 <- bson.getAsTry[String]("time24")
      track <- bson.getAsTry[String]("track")
      trainer <- bson.getAsUnflattenedTry[String]("trainer")
      updatedAt <- bson.getAsTry[OffsetDateTime]("updatedAt")
      markers <- bson.getAsUnflattenedTry[Markers]("markers")
      tweets <- bson.getAsUnflattenedTry[List[String]]("tweets")
      bfSp <- bson.getAsUnflattenedTry[String]("bfSp")
      bfWinReturn <- bson.getAsUnflattenedTry[String]("bfWinReturn")
      ewReturn <- bson.getAsUnflattenedTry[String]("ewReturn")
      indSp <- bson.getAsUnflattenedTry[String]("indSp")
      place <- bson.getAsUnflattenedTry[String]("place")
      spWinReturn <- bson.getAsUnflattenedTry[String]("spWinReturn")
      winningDist <- bson.getAsUnflattenedTry[String]("winningDist")
    } yield new Entry(_id, age, country, createdAt, date, firstAppearance, jockey, marketStartTime, name,
      runners, time24, track, trainer, updatedAt, markers, tweets, bfSp, bfWinReturn, ewReturn, indSp,
      place, spWinReturn, winningDist)
  }
}