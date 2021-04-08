package net.carboninter.models

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

class EntryTest extends AnyWordSpecLike {


  val json =
    """{
      |    "_id" : "2021-04-06_16:35_gb_pontefract_little ted",
      |    "age" : 4,
      |    "country" : "gb",
      |    "createdAt" : "2021-04-04T11:17:09.806Z",
      |    "date" : "2021-04-06",
      |    "firstAppearance" : "2021-04-04T11:17:09.805Z",
      |    "jockey" : "david allan",
      |    "marketStartTime" : "2021-04-06T15:35:00.000Z",
      |    "name" : "little ted",
      |    "runners" : 14,
      |    "time24" : "16:35",
      |    "track" : "pontefract",
      |    "trainer" : "tim easterby",
      |    "updatedAt" : "2021-04-04T21:17:14.989Z"
      |}
      |""".stripMargin

  val json2 =
    """{
      |    "_id" : "2020-10-28_13:15_gb_nottingham_little ted",
      |    "age" : 3,
      |    "country" : "gb",
      |    "createdAt" : "2020-10-26T14:21:21.167Z",
      |    "date" : "2020-10-28",
      |    "firstAppearance" : "2020-10-26T14:21:21.165Z",
      |    "jockey" : "james sullivan",
      |    "marketStartTime" : "2020-10-28T13:15:00.000Z",
      |    "name" : "little ted",
      |    "runners" : 12,
      |    "time24" : "13:15",
      |    "track" : "nottingham",
      |    "trainer" : "tim easterby",
      |    "updatedAt" : "2021-03-26T00:04:28.129Z",
      |    "markers" : {
      |        "batch" : "2102A"
      |    },
      |    "tweets" : [
      |        "1321437786260574211"
      |    ],
      |    "bfSp" : "21.05",
      |    "bfWinReturn" : "-10.00",
      |    "ewReturn" : "-10.00",
      |    "indSp" : "19.00",
      |    "place" : "4",
      |    "spWinReturn" : "-10.00",
      |    "winningDist" : "10"
      |}
      |""".stripMargin


  "Parsing an entry" should {

    "produce a model" in {

//      println(Json.parse(json).as[Entry])

    }
  }
}
