package net.carboninter.metrics

import io.prometheus.client.{Counter, Gauge, SimpleTimer, Summary}
import io.prometheus.client.exporter.HTTPServer

object Metrics {

  val streamedTweetCounter = Counter.build()
    .name("tg_streamed_tweet_total")
    .help(s"Total incoming streamed tweets")
    .labelNames("track")
    .register()

  val requestCounter = Counter.build()
    .name("tg_twitter_stream_request_total")
    .help(s"Total twitter stream requests")
    .labelNames("status")
    .register()

  val metricsServer = new HTTPServer(9416)
}
