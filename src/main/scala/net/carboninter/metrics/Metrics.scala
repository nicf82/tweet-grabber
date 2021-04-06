package net.carboninter.metrics

import io.prometheus.client.{Counter, Gauge, SimpleTimer, Summary}
import io.prometheus.client.exporter.HTTPServer

object Metrics {

//  val dayRequestsCounter = Counter.build().name("frbd_day_requests_total").help("Total days requested").register()
//  val entriesWithoutResultsCounter = Counter.build().name("frbd_entries_without_results_processed_total").help("Total entries without results processed").register()
//  val entriesWithResultsCounter = Counter.build().name("frbd_entries_with_results_processed_total").help("Total entries with results processed").register()
//
//  val rbdRequestsCounter = Counter.build().name("frbd_rbd_requests_total").help("Total days requested from rbd").register()
//
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
