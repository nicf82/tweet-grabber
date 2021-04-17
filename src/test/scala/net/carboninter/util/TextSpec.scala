package net.carboninter.util

import net.carboninter.models.StubTweet
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers._

import java.time.OffsetDateTime

class TextSpec extends AnyWordSpecLike {

  val simulatedNow = OffsetDateTime.parse("2020-01-30T10:00:00Z")

  "Matching entries to tweets, when the tweet contains the track name" should {

    "match if the name is contained in the tweet but without any whitespace (e.g. as a hashtag)" in {

      val lowerText = "the wonderful #horseymchorseface should do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe true
    }

    "match if the name is contained in the tweet and is surrounded by whitespace" in {

      val lowerText = "the wonderful horsey mchorseface should do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe true
    }

    "match if the name is at the beginning of the tweet and is followed by whitespace" in {

      val lowerText = "horsey mchorseface should do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe true
    }

    "NOT match if the name is at the beginning of the tweet and is not followed by whitespace" in {

      val lowerText = "horsey mchorsefaceshould do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe false
    }

    "NOT match if the name is contained in the tweet and is adjoined at the start and followed by whitespace" in {

      val lowerText = "the wonderfulhorsey mchorseface should do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe false
    }

    "NOT match if the name is contained in the tweet and is adjoined at the start and end" in {

      val lowerText = "the wonderfulhorsey mchorsefaceshould do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe false
    }

    "NOT match if the name is contained in the tweet and is adjoined at the end and preceeded by whitespace" in {

      val lowerText = "the wonderful horsey mchorsefaceshould do well in the 3:30 at market rasen"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe false
    }

    "NOT match if the name is at the end of the tweet and is not preceeded by whitespace" in {

      val lowerText = "The 3:30 at market rasen tip ishorsey mchorseface"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe false
    }

    "match if the name is at the end of the tweet and is preceeded by whitespace" in {

      val lowerText = "The 3:30 at market rasen tip is horsey mchorseface"

      Text.matchEntryToTweet("horsey mchorseface", "market rasen", lowerText) shouldBe true
    }
  }
}
