package net.carboninter

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.scaladsl.{Flow, Keep, RetryFlow, RunnableGraph, Sink, Source}
import akka.stream.{ActorAttributes, KillSwitches, Supervision, SystemMaterializer, UniqueKillSwitch}
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.metrics.Metrics.metricsServer
import net.carboninter.models.StubTweet
import net.carboninter.repos._
import net.carboninter.services.TweetProcessingService
import net.carboninter.util.Logging
import play.api.libs.json.JsObject
import reactivemongo.api.AsyncDriver

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object Application extends App with Logging {

  val config: Config = ConfigFactory.load()
  val driver = new AsyncDriver()

  // Create Akka system for thread and streaming management
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val materializer = SystemMaterializer(actorSystem).materializer


  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

  val connector = new TwitterConnector(config)

  val dbProvider = new DbProvider(driver, config)

  val tweetRepo = new TweetRepo(dbProvider)
  val newTweetRepo = new NewTweetRepo(dbProvider)
  val tweetStubRepo = new TweetStubRepo(dbProvider)
  val entryRepo = new EntryRepo(dbProvider)

  val service = new TweetProcessingService(tweetRepo, newTweetRepo, entryRepo, tweetStubRepo)


  val count = 1000
  val startedAt = System.currentTimeMillis()


//  val source: Source[NotUsed, Cancellable] = Source.tick(0.seconds, 0.seconds, NotUsed).take(10)

  val wireTapSink = Sink.fold[Int, (StubTweet,JsObject)](0) {
    case (acc, _) => acc + 1
  }

//  val flow = Flow[NotUsed]
//    .flatMapConcat { _ =>
//      logger.info(s"Requesting batch of $count")
//      service.standardTweetProcessingStream(200, wireTapSink)
//    }
//
//  val (cancellable, fcount) = source
//    .viaMat(flow)(Keep.left)
//    .toMat(Sink.ignore)(Keep.both)
//    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
//    .run()


//  service.standardTweetProcessingStream(count, wireTapSink)

  val (killSwitch, tweetCount) = tweetRepo.tweetsSource(2)
    .viaMat(KillSwitches.single)(Keep.right)
    //.take(count)
    .via(service.standardIncomingTweetFlow)
    .wireTapMat(wireTapSink)(Keep.both)
    .via(service.markTweetProcessedFlow)
    .viaMat(service.tweetEventMatchingFlow)(Keep.left)
    .toMat(Sink.ignore)(Keep.left)
    .run()


  val result = tweetCount.map { count =>
    val elapsed: Double = ((System.currentTimeMillis().toDouble-startedAt)/1000)
    logger.info("Tweets processed: " + count + " in " + elapsed + " seconds so " + (count/elapsed) + " per second")
    actorSystem.terminate()
  }

  actorSystem.registerOnTermination {
    logger.info("ActorSystem terminating...")
    System.exit(0)
  }

  sys.addShutdownHook {
    logger.info("System shutting down...")
    metricsServer.stop()
    killSwitch.shutdown()
    Await.result(result, Duration.Inf)
  }

}
