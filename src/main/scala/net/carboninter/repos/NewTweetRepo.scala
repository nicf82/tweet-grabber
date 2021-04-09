package net.carboninter.repos

import _root_.reactivemongo.api.bson.{BSONDocument, _}
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import net.carboninter.util.Logging
import play.api.libs.json.JsObject
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.api.WriteConcern
import reactivemongo.api.bson.collection._
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.json2bson._
import reactivemongo.play.json.compat.lax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class NewTweetRepo(dbProvider: DbProvider) extends MongoFormats with Logging {

  lazy val tweetsCollection = for {
    db <- dbProvider.db
    collection = db.collection[BSONCollection]("new_tweets")
    _ = collection.indexesManager.ensure(Index(Seq("timestamp_ms" -> IndexType.Ascending)))
  } yield collection

  def count(): Future[Long] = {

    val query = BSONDocument()

    tweetsCollection.flatMap { coll =>
      coll.count(Some(query))
    }
  }

  def tweetsSource()(implicit m: Materializer): Source[JsObject, Future[NotUsed]] = {

    val query = BSONDocument()
    val sortBy = BSONDocument("timestamp_ms" -> 1)

    Source.futureSource {
      tweetsCollection.map { coll =>
        val cursor: AkkaStreamCursor.WithOps[JsObject] = coll
          .find(query).sort(sortBy)
          .cursor[JsObject]()

        cursor.documentSource().mapMaterializedValue(_ => NotUsed)
      }
    }
  }

  def store(id: String, json: JsObject): Future[WriteResult] = {

    val query = BSONDocument("_id" -> id)

    for {
      coll <- tweetsCollection
      updateWriteResult <- coll.update.one(query, json, upsert = true, multi = false)
    } yield updateWriteResult
  }
}
