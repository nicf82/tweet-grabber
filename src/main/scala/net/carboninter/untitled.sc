import java.time.{Duration, Instant}
import java.time.temporal.TemporalAmount

val d = Instant.ofEpochMilli(1594650733685L)

val d1 = d.minus(Duration.ofDays(7))

d1.toEpochMilli
