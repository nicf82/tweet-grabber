package net.carboninter.repos

import java.time.OffsetDateTime

import reactivemongo.api.bson.BSONDateTime

trait MongoFormats {

  implicit class MongoOffsetDateTime(offsetDateTime: OffsetDateTime) {
    def toMongo = BSONDateTime(offsetDateTime.toInstant.toEpochMilli)
  }
}
