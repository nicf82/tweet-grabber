package net.carboninter.repos

import _root_.reactivemongo.api.bson.{BSONDocument, _}
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
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

class TweetStubRepo(driver: AsyncDriver, config: Config) extends MongoFormats with Logging {

  lazy val connection = driver.connect(config.getString("database.url"))

  lazy val db = for {
    conn <- connection
    db <- conn.database(config.getString("database.name"))
  } yield db

  lazy val collection = for {
    db <- db
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

  def store(id: String, text: String): Future[WriteResult] = {

    val query = BSONDocument("_id" -> id)
    val update = BSONDocument("text" -> text)

    for {
      coll <- collection
      updateWriteResult <- coll.update.one(query, update, upsert = true, multi = false)
    } yield updateWriteResult
  }
}
