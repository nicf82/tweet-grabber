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
      tweet1.as[StubTweet] shouldBe StubTweet(
        "1195770077150363649",
        """16/11/2019 18:15 Wolverhampton
          |
          |1⃣ Moment Of Silence
          |2⃣ Emily Goldfinch
          |3⃣
          |
          |#MomentOfSilence""".stripMargin,OffsetDateTime.parse("2019-11-16T18:26:17.383Z"))

    }
  }
}
