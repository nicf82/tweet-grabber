package net.carboninter.metrics

import io.prometheus.client.{Counter, Gauge, SimpleTimer, Summary}
import io.prometheus.client.exporter.HTTPServer

object Metrics {

  val incomingTweetsCounter = Counter.build().name("tg_incoming_tweets_total").help("Total incoming tweets for processing").register()
  val storedStubTweetsCounter = Counter.build().name("tg_stored_stub_tweets_total").help("Total tweet stubs stored").register()
  val storedFullTweetsCounter = Counter.build().name("tg_stored_full_tweets_total").help("Total full tweets stored").register()
  val stubTweetParseFailureCounter = Counter.build().name("tg_parse_failed_stub_tweets_total").help("Total tweet stubs failed to parse").register()

  val entriesMatchedCounter = Counter.build().name("tg_entries_matched_to_tweet_total").help("Total entries matched to a tweet").register()
  val entriesReturnedCounter = Counter.build().name("tg_entries_returned_for_tweet_total").help("Total entries returned as potential tweet matches").register()
  val entryParseFailureCounter = Counter.build().name("tg_parse_failed_entry_total").help("Total entries failed to parse").register()
  val tweetsAttachedCounter = Counter.build().name("tg_tweets_attached_to_entry_total").help("Total tweets attached to entries").register()

//  val requestLatency = Summary.build()
//    .name("frbd_rbd_request_latency_seconds")
//    .help("RBD HTTP Request latency in seconds.")
//    .register();
//
//  def timeRbdRequest[T](block: => T): T = {
//    rbdRequestsCounter.inc()
//    val requestTimer = new SimpleTimer()
//    try {
//      block
//    } finally {
//      requestLatency.observe(requestTimer.elapsedSeconds())
//    }
//  }

  val metricsServer = new HTTPServer(9416)
}
