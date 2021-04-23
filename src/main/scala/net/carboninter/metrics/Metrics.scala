package net.carboninter.metrics

import io.prometheus.client.Counter
import io.prometheus.client.exporter.HTTPServer

object Metrics {

  val tweetTrackMentionCounter = Counter.build()
    .name("tg_tweet_track_total")
    .help(s"Total mentions of a track in tweets")
    .labelNames("track")
    .register()

  val tweetParsedCounter = Counter.build()
    .name("tg_tweet_parsed")
    .help(s"Total incoming parsed tweets")
    .register()

  val unparsableFrameCounter = Counter.build()
    .name("tg_frame_not_parsable")
    .help(s"Total frames that could not be parsed by reason")
    .labelNames("reason")
    .register()

  val twitterKeepAliveCrCounter = Counter.build()
    .name("tg_twitter_keepalive_cr_total")
    .help(s"Total number of keepalive carriage returns received in Twitter stream")
    .register()

  val streamConnectResponseCounter = Counter.build()
    .name("tg_twitter_stream_connect_response_total")
    .help(s"Total twitter stream connect responses by status")
    .labelNames("status")
    .register()

//  val streamInterruptionCounter = Counter.build()
//    .name("tg_twitter_stream_interruption_total")
//    .help(s"Total twitter stream interruptions by reason")
//    .labelNames("reason")
//    .register()
//
//  val streamConnectFailureCounter = Counter.build()
//    .name("tg_twitter_stream_connect_failure_total")
//    .help(s"Total twitter stream connect failures by reason")
//    .labelNames("reason")
//    .register()

  val metricsServer = new HTTPServer(9416)
}
