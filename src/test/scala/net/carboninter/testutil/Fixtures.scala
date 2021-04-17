package net.carboninter.testutil

import play.api.libs.json.{JsObject, Json}

trait Fixtures {

  def jsResource(path: String) = Json.parse(scala.io.Source.fromInputStream(getClass.getResource(s"/fixtures/$path").openStream()).getLines.mkString).as[JsObject]

  val tweet1 = jsResource("tweets/1195770077150363649.json")
  val tweet2 = jsResource("tweets/1195704270743654400.json")
  val tweet3 = jsResource("tweets/1380086687682985984.json")
}
