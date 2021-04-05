package net.carboninter.util

import org.scalatest.wordspec.AnyWordSpecLike

class DateTimeUtilsTest extends AnyWordSpecLike {

  "Converting a date and time string" should {

    "work ok" in {

      println(DateTimeUtils.toOffsetDateTime("2019-12-26", "12:00pm"))
    }
  }

}
