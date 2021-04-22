package net.carboninter.services

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.typesafe.config.Config
import net.carboninter.TwitterStreamPublisher.logger
import net.carboninter.actors.TwitterTermsActor
import net.carboninter.actors.TwitterTermsActor.{GetState, SetState}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.metrics.Metrics
import net.carboninter.models.{StubTweet, TwitterTermsCommand}
import net.carboninter.util.Logging

import scala.concurrent.duration._

class TwitterService(config: Config)(implicit actorSystem: ActorSystem) extends Logging {

  import actorSystem.dispatcher
  implicit val askTimeout = Timeout(5.seconds)

  val twitterConnector = new TwitterConnector(config)

  val ttaRef = actorSystem.actorOf(Props(new TwitterTermsActor))

  val setTwitterTermsStateFlow = Flow[TwitterTermsCommand]
    .map { command =>
      SetState(command.liveTracks.map(_.track).sorted)
    }
    .ask[List[String]](ttaRef)

  val tweetStream = Flow[GetState.type]
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
          logger.info("Terms have changed, ending current twitter stream")
          t == terms
        }
        .map { case (js, terms) =>

          val lowerText = js.as[StubTweet].text.toLowerCase

          //We found a tweet containing a track name, log metric to see which tracks are found most often
          for(track <- terms if lowerText.contains(track)) {
            Metrics.tweetTrackMentionCounter.labels(track).inc()
          }

          (js, terms, lowerText)
        }
    }

  val termsSink = Sink.fold[List[String], List[String]](Nil) {
    case (acc, t) if acc == t =>
      acc
    case (_, t) =>
      logger.info("New terms set: " + t.mkString(", "))
      t
  }
}
