package net.carboninter.repos

import _root_.reactivemongo.api.bson.{BSONDocument, _}
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import net.carboninter.models.StubTweet
import net.carboninter.util.Logging
import play.api.libs.json.{JsObject, Json}
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.api.bson.collection._
import reactivemongo.api.collections.GenericCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{AsyncDriver, CollectionMetaCommands, WriteConcern}
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.json2bson._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TweetStubRepo(dbProvider: DbProvider) extends MongoFormats with Logging {

  lazy val collection = for {
    db <- dbProvider.db
    collection = db.collection[BSONCollection]("tweet_stubs")
  } yield collection


  def count(): Future[Long] = {

    collection.flatMap { coll =>
      coll.count()
    }
  }

  def tweetStubsSource()(implicit m: Materializer): Source[JsObject, Future[NotUsed]] = {

    val query = BSONDocument()

    Source.futureSource {
      collection.map { coll =>
        val cursor: AkkaStreamCursor.WithOps[JsObject] = coll
          .find(query)
          .cursor[JsObject]()

        cursor.documentSource().mapMaterializedValue(_ => NotUsed)
      }
    }
  }

  def store(stubTweet: StubTweet): Future[WriteResult] = {

    val query = BSONDocument("_id" -> stubTweet.id_str)
    val update = BSONDocument("text" -> stubTweet.text, "timestamp" -> stubTweet.time)

    for {
      coll <- collection
      updateWriteResult <- coll.update.one(query, update, upsert = true, multi = false)
    } yield updateWriteResult
  }



  def incEntryErrors(tweetId: String): Future[WriteResult] = {

    val query = BSONDocument("_id" -> tweetId)
    val update = BSONDocument("$inc" -> BSONDocument("entryErrors" -> 1))

    for {
      coll <- collection
      updateWriteResult <- coll.update.one(query, update, upsert = false, multi = false)
    } yield updateWriteResult
  }
}
