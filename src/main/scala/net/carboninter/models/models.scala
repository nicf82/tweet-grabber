package net.carboninter.models

package net.carboninter

import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONReader, BSONString, BSONWriter, Macros}

import java.time.Instant

case class ExtendedTweet(full_text: String)
object ExtendedTweet {
  implicit val extendedTweetHandler: BSONDocumentHandler[ExtendedTweet] = Macros.handler[ExtendedTweet]
}

case class Tweet(
  created_at: String,
  id_str: String
)

object Tweet {

  implicit val instantReader: BSONReader[Instant] = BSONReader.collect[Instant] {
    case BSONString(str) => Instant.ofEpochMilli(str.toLong)
  }

  implicit val tweetHandler: BSONDocumentHandler[Tweet] = Macros.handler[Tweet]
}
