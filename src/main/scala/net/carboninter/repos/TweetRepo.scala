package net.carboninter.repos

import _root_.reactivemongo.api.bson.{BSONDocument, _}
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import net.carboninter.util.Logging
import play.api.libs.json.{JsObject, Json}
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.api.{Cursor, WriteConcern}
import reactivemongo.api.bson.collection._
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.compat._
import reactivemongo.play.json.compat.bson2json._
import reactivemongo.play.json.compat.json2bson._
import reactivemongo.play.json.compat.lax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future



class TweetRepo(dbProvider: DbProvider) extends MongoFormats with Logging {

  lazy val tweetsCollection = for {
    db <- dbProvider.db
    collection = db.collection[BSONCollection]("tweets")
    _ = collection.indexesManager.ensure(Index(Seq("timestamp_ms" -> IndexType.Ascending)))
    _ = collection.indexesManager.ensure(Index(Seq("batchIdent" -> IndexType.Ascending)))
    _ = collection.indexesManager.ensure(Index(Seq("postStatus" -> IndexType.Ascending)))
  } yield collection


  def setBatchIdent(id: String, batch: Int)(implicit m: Materializer): Future[WriteResult] = {
    tweetsCollection.flatMap { coll =>
      coll
        .update(WriteConcern.Journaled)
        .one(BSONDocument("_id" -> id), BSONDocument("$set" -> BSONDocument("batchIdent" -> batch)))
    }
  }

  def count(currentBatchIdent: Int): Future[Long] = {

    val query = BSONDocument(
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

    tweetsCollection.flatMap { coll =>
      coll.count(Some(query))
    }
  }

  def tweetsSource(currentBatchIdent: Int)(implicit m: Materializer): Source[JsObject, Future[NotUsed]] = {

    val query = BSONDocument(
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

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

  def countFromTime(currentBatchIdent: Int, startAt: String): Future[Long] = {

    val query = BSONDocument(
      "timestamp_ms" -> BSONDocument("$gt" -> startAt),
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

    tweetsCollection.flatMap { coll =>
      coll.count(Some(query))
    }
  }

  def allTweetsSource()(implicit m: Materializer): Source[JsObject, Future[NotUsed]] = {

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

  def tweetsSourceFromTime(currentBatchIdent: Int, startAt: String)(implicit m: Materializer): Source[JsObject, Future[NotUsed]] = {

    val query = BSONDocument(
      "timestamp_ms" -> BSONDocument("$gt" -> startAt),
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

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

  def get(id: String) = {

    val query = BSONDocument("_id" -> id)

    tweetsCollection.flatMap { coll =>
      val cursor: Cursor.WithOps[JsObject] = coll
        .find(query).sort(BSONDocument("marketStartTime" -> 1))
        .cursor[JsObject]()
      cursor.collect[List](1, Cursor.FailOnError()).map(_.headOption)
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
