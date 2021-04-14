package net.carboninter.testutil

import net.carboninter.models.{Entry, Markers}
import play.api.libs.json.{JsObject, Json}

import java.time.OffsetDateTime
import scala.util.Try

trait Fixtures {

  implicit val format = Json.format[Markers]

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val largeCaseClassReads: Reads[Entry] = {

    val r1 = {
      (__ \ "_id").read[String] and
      (__ \ "age").readNullable[Int] and
      (__ \ "country").read[String] and
      (__ \ "createdAt").read[OffsetDateTime] and
      (__ \ "date").read[String] and
      (__ \ "firstAppearance").read[OffsetDateTime] and
      (__ \ "jockey").readNullable[String] and
      (__ \ "marketStartTime").read[OffsetDateTime] and
      (__ \ "name").read[String] and
      (__ \ "runners").read[Int] and
      (__ \ "time24").read[String] and
      (__ \ "track").read[String]
    }.tupled

    val r2 = {
      (__ \ "trainer").readNullable[String] and
      (__ \ "updatedAt").read[OffsetDateTime] and
      (__ \ "markers").readNullable[Markers] and
      (__ \ "tweets").readNullable[List[String]] and
      (__ \ "bfSp").readNullable[String] and
      (__ \ "bfWinReturn").readNullable[String] and
      (__ \ "ewReturn").readNullable[String] and
      (__ \ "indSp").readNullable[String] and
      (__ \ "place").readNullable[String] and
      (__ \ "spWinReturn").readNullable[String] and
      (__ \ "winningDist").readNullable[String]
    }.tupled

    (r1 and r2).tupled.map {
      case ((a, b, c, d, e, f, g, h, i, j, k, l), (m, n, o, p, q, r, s, t, u, v, w)) =>
        Entry(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w)
    }
  }

  def jsResource(path: String) = Json.parse(scala.io.Source.fromInputStream(getClass.getResource(s"/fixtures/$path").openStream()).getLines.mkString).as[JsObject]

  val tweet1 = jsResource("tweets/1195770077150363649.json")
  val tweet2 = jsResource("tweets/1195704270743654400.json")
  val tweet3 = jsResource("tweets/1380086687682985984.json")

  val entry1a = Try(jsResource("entries/2019-11-16_18:15_gb_wolverhampton_moment of silence.json").as[Entry])
  val entry1b = Try(jsResource("entries/2019-11-16_18:15_gb_wolverhampton_emily goldfinch.json").as[Entry])

  val corruptEntry = Try(Json.obj("ill_try_but" -> "this wont be parsable as an entry").as[Entry])
}
