package net.carboninter.testutil

import net.carboninter.models.Entry
import play.api.libs.json.{JsObject, Json}

import scala.util.Try

trait Fixtures {

  def jsResource(path: String) = Json.parse(scala.io.Source.fromInputStream(getClass.getResource(s"/fixtures/$path").openStream()).getLines.mkString).as[JsObject]

  val tweet1 = jsResource("tweets/1195770077150363649.json")
  val tweet2 = jsResource("tweets/1195704270743654400.json")

  val entry1a = Try(jsResource("entries/2019-11-16_18:15_gb_wolverhampton_moment of silence.json").as[Entry])
  val entry1b = Try(jsResource("entries/2019-11-16_18:15_gb_wolverhampton_emily goldfinch.json").as[Entry])

  val corruptEntry = Try(Json.obj("ill_try_but" -> "this wont be parsable as an entry").as[Entry])
}
