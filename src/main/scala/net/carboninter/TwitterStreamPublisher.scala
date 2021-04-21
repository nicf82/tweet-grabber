package net.carboninter

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.actors.TwitterTermsActor.GetState
import net.carboninter.metrics.Metrics.metricsServer
import net.carboninter.services.{MqttService, TwitterService}
import net.carboninter.util.{Logging, Text}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object TwitterStreamPublisher extends App with Logging {

  val config: Config = ConfigFactory.load()

  // Create Akka system for thread and streaming management
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val materializer: Materializer = SystemMaterializer(actorSystem).materializer


  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Resume
  }

  val twitterService = new TwitterService(config)
  val mqttService = new MqttService(config)

  val (commandStreamConnected, commandStreamKS) = mqttService.twitterTermsCommandSource
    .viaMat(KillSwitches.single)(Keep.both)
    .via(twitterService.setTwitterTermsStateFlow)
    .to(Sink.foreach(println))
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .run()

  val tweetStreamKillSwitch = Source.tick(0.millis, 500.millis, GetState)
    .viaMat(KillSwitches.single)(Keep.right)
    .via(twitterService.tweetStream)
    .map { case (js, terms, lowerText) =>
      logger.debug("Publishing: " + Text.hilight(lowerText, terms: _*))
      js
    }
    .to(mqttService.publishTweetSink)
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .run()


  val result = commandStreamConnected.map { _ =>
    logger.info("MQTT Connected, listening for twitter terms")
    //actorSystem.terminate()
  }

  actorSystem.registerOnTermination {
    logger.info("ActorSystem terminating...")
    System.exit(0)
  }

  sys.addShutdownHook {
    logger.info("System shutting down...")
    metricsServer.stop()
    commandStreamKS.shutdown()
    tweetStreamKillSwitch.shutdown()
    Await.result(result, Duration.Inf)
  }

}
