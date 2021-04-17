package net.carboninter

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorAttributes, KillSwitches, Materializer, Supervision, SystemMaterializer}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.actors.TwitterTermsActor
import net.carboninter.actors.TwitterTermsActor.{GetState, SetState}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.metrics.Metrics
import net.carboninter.metrics.Metrics.metricsServer
import net.carboninter.models.StubTweet
import net.carboninter.services.{MqttService, TweetProcessingService}
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
  implicit val askTimeout = Timeout(5.seconds)


  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

  val twitterConnector = new TwitterConnector(config)

  val mqttService = new MqttService(config)

  val ttaRef = actorSystem.actorOf(Props(new TwitterTermsActor))

  val (commandStreamDone, commandStreamKS) = mqttService.twitterTermsCommandSource
    .viaMat(KillSwitches.single)(Keep.both)
    .map { command =>
      SetState(command.liveTracks.map(_.track).sorted)
    }
    .ask[List[String]](ttaRef)
    .to(Sink.foreach(println))
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .run()

  val tweetStreamKillSwitch = Source.repeat(GetState)
    .viaMat(KillSwitches.single)(Keep.right)
    .ask[List[String]](ttaRef)
    .mapConcat {
      case Nil => None
      case terms => Some(terms)
    }
    .flatMapConcat { terms =>
      twitterConnector
        .tweetSource(terms.mkString(","))
        .zip(Source.repeat(GetState).ask[List[String]](ttaRef))
        .takeWhile { case (_, t) => //Continue to consume the stream as long as terms have not changed
          t == terms
        }
        .map { case (js, terms) =>

          val lowerText = js.as[StubTweet].text.toLowerCase

          //We found a tweet containing a track name, log metric to see which tracks are found most often
          for(track <- terms if lowerText.contains(track)) {
            Metrics.streamedTweetCounter.labels(track).inc()
          }

          (js, terms, lowerText)
        }
    }
    .map { case (js, terms, lowerText) =>
      logger.debug("Publishing: " + Text.hilight(lowerText, terms: _*))
      js
    }
    .to(mqttService.publishTweetSink)
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .run()


  val result = commandStreamDone.map { _ =>
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
    commandStreamKS.shutdown()
    tweetStreamKillSwitch.shutdown()
    Await.result(result, Duration.Inf)
  }

}
