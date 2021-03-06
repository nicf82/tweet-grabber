package net.carboninter.models

import org.scalatest.matchers.should.Matchers._
import net.carboninter.testutil.Fixtures
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.OffsetDateTime

class StubTweetSpec extends AnyWordSpecLike with Fixtures {

  "Parsing a" should {
    "read an extended tweet's full text field and others correctly" in {
      tweet2.as[StubTweet] shouldBe StubTweet(
        "1195704270743654400",
        "13:50 #Cheltenham result: 1. West Approach 11/4F (3l), 2. Achille 10/1, 3. Potters Legend 7/1, 4. Ramses De Teillee 11/4F https://t.co/JMJV7COHyI",
        OffsetDateTime.parse("2019-11-16T14:04:47.913Z")
      )
    }

    "read a non extended tweet's text field and others correctly" in {
      tweet3.as[StubTweet] shouldBe StubTweet(
        "1380086687682985984",
        """LATEST NapsLeagueH-2-H ENTRY:
          |@d40tomo
          |The Shunter (13:45) Aintree
          |Evas Diva (14:30) Taunton
          |Tiger Roll (14:50) Aintree
          |Batchelor Boy (15:15) Southwell
          |Frero Banbou (16:40) Aintree
          |Letsbe Avenue (15:05) Taunton
          | https://t.co/yg3hefvYWE
          |
          |#NLentry #GoRacing""".stripMargin,OffsetDateTime.parse("2021-04-08T09:14:41.771Z")
      )

    }
  }
}
