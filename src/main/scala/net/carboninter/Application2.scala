package net.carboninter

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorAttributes, KillSwitches, SharedKillSwitch, Supervision, SystemMaterializer, UniqueKillSwitch}
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.Application.logAndStopDecider
import net.carboninter.connectors.TwitterConnector
import net.carboninter.metrics.Metrics.metricsServer
import net.carboninter.models.StubTweet
import net.carboninter.repos._
import net.carboninter.services.{MqttService, TweetProcessingService}
import net.carboninter.util.Logging
import play.api.libs.json.{JsObject, JsValue}
import reactivemongo.api.AsyncDriver

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object Application2 extends App with Logging {

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

  val tweetProcessingService = new TweetProcessingService(tweetRepo, newTweetRepo, entryRepo, tweetStubRepo)
  val mqttService = new MqttService(config)

//  val sharedKillSwitch = Some(KillSwitches.shared("ks-" + Random.alphanumeric.take(6).mkString))
//  connector.tweetSource(sharedKillSwitch.get, "bitcoin")
//    .to(Sink.foreach(j => println(j)))
//    .run()

  val (done, killSwitch) = mqttService.twitterTermsCommandSource
    .viaMat(KillSwitches.single)(Keep.both)
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .statefulMapConcat { () =>
      var sharedKillSwitch: Option[SharedKillSwitch] = None

      command => {
        sharedKillSwitch.map(_.shutdown())

        sharedKillSwitch = Some(KillSwitches.shared("ks-" + Random.alphanumeric.take(6).mkString))
        Some(connector.tweetSource(sharedKillSwitch.get, command.liveTracks.map(_.track).mkString(",")))

      }
    }
    .flatMapConcat(identity)
    .to(Sink.foreach(println))
    .run()


  val result = done.map { _ =>
    logger.info("Started")
    //actorSystem.terminate()
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
