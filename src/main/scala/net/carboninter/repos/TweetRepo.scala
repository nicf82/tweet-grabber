package net.carboninter.repos

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import net.carboninter.models.net.carboninter.Tweet
import net.carboninter.util.Logging
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.api.{AsyncDriver, WriteConcern}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, _}
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TweetRepo(driver: AsyncDriver, config: Config) extends MongoFormats with Logging {

  lazy val connection = driver.connect(config.getString("database.url"))

  lazy val db = for {
    conn <- connection
    db <- conn.database(config.getString("database.name"))
  } yield db

  lazy val tweetsCollection = for {
    db <- db
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
      "timestamp_ms" -> BSONDocument("$gt" -> "1594650733685"), //Tweets before this had retweets so can be handled
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

    tweetsCollection.flatMap { coll =>
      coll.count(Some(query))
    }
  }

  def tweetsSource(currentBatchIdent: Int)(implicit m: Materializer): Source[Tweet, Future[NotUsed]] = {

    val query = BSONDocument(
      "timestamp_ms" -> BSONDocument("$gt" -> "1594650733685"), //Tweets before this had retweets so can be handled
      "batchIdent" -> BSONDocument("$lt" -> currentBatchIdent)
    )

    Source.futureSource {
      tweetsCollection.map { coll =>
        val cursor: AkkaStreamCursor.WithOps[Tweet] = coll
          .find(query)
          .cursor[Tweet]()
        cursor.documentSource().mapMaterializedValue(_ => NotUsed)
      }
    }
  }
}
