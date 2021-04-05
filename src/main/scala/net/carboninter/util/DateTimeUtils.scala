package net.carboninter.util

import java.time.{LocalDateTime, OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

  //Failed (but not in tests???) when parsing 12:00PM, unless Locale is set
  val ukDtf = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mma", new Locale("en", "UK"))
  val londonZone = ZoneId.of("Europe/London")

  def toOffsetDateTime(date: String, time: String): OffsetDateTime =
    LocalDateTime.parse(date + " " + time.toUpperCase, ukDtf).atZone(londonZone).toOffsetDateTime

}
