package net.carboninter.repos

import _root_.reactivemongo.api.bson._
import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import net.carboninter.models.{Entry, LiveEvent}
import net.carboninter.util.Logging
import org.xml.sax.ErrorHandler
import play.api.libs.json.JsObject
import reactivemongo.akkastream.AkkaStreamCursor
import reactivemongo.api.bson.collection._
import reactivemongo.api.{AsyncDriver, Cursor}
import reactivemongo.akkastream.{AkkaStreamCursor, cursorProducer}
import reactivemongo.api.bson.{BSONDocument, _}
import reactivemongo.api.bson.collection._
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{AsyncDriver, WriteConcern}
import reactivemongo.play.json.compat.json2bson._

import javax.swing.SortOrder
import reactivemongo.api._
import reactivemongo.api.indexes.{Index, IndexType}

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future




class EntryRepo(driver: AsyncDriver, config: Config) extends MongoFormats with Logging {

  lazy val connection = driver.connect(config.getString("database.url"))

  val collectionName = "entries"

  lazy val db = for {
    conn <- connection
    db <- conn.database(config.getString("database.name"))
  } yield db



  lazy val entriesCollection = for {
    db <- db
    collection = db.collection[BSONCollection](collectionName)
    _ = collection.indexesManager.ensure(Index(Seq("firstAppearance" -> IndexType.Ascending, "marketStartTime" -> IndexType.Ascending)))
    _ = collection.indexesManager.ensure(Index(Seq("track" -> IndexType.Ascending, "name" -> IndexType.Ascending, "date" -> IndexType.Ascending, "time24" -> IndexType.Ascending)))
    _ = collection.indexesManager.ensure(Index(Seq("markers.batch" -> IndexType.Ascending)))
  } yield collection


  def getEntries(minFirstAppearance: OffsetDateTime, maxMarketStartTime: OffsetDateTime) = {

    val query = BSONDocument("$and" -> BSONArray(
      BSONDocument("firstAppearance" -> BSONDocument("$lte" -> minFirstAppearance.toBSON)),
      BSONDocument("marketStartTime" -> BSONDocument("$gt" -> maxMarketStartTime.toBSON))
    ))

    entriesCollection.flatMap { coll =>
      val cursor: Cursor.WithOps[BSONDocument] = coll
        .find(query).sort(BSONDocument("marketStartTime" -> 1))
        .cursor[BSONDocument]()
      cursor.collect[List](1000, Cursor.FailOnError()).map(_.map(_.asTry[Entry]))
    }

  }

  def getEntriesStream(minFirstAppearance: OffsetDateTime, maxMarketStartTime: OffsetDateTime)(implicit m: Materializer) = {

    val query = BSONDocument("$and" -> BSONArray(
      BSONDocument("firstAppearance" -> BSONDocument("$lte" -> minFirstAppearance.toBSON)),
      BSONDocument("marketStartTime" -> BSONDocument("$gt" -> maxMarketStartTime.toBSON))
    ))

    Source.futureSource {
      entriesCollection.map { coll =>
        val cursor: AkkaStreamCursor.WithOps[BSONDocument] = coll
          .find(query).sort(BSONDocument("marketStartTime" -> 1))
          .cursor[BSONDocument]()
        cursor.documentSource().mapMaterializedValue(_ => NotUsed).map(_.asTry[Entry])
      }
    }
  }

}
