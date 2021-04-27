package net.carboninter.services

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.RestartSettings
import akka.stream.scaladsl.{Flow, Framing, Keep, Merge, RestartSource, Sink, Source}
import akka.util.{ByteString, Timeout}
import com.typesafe.config.Config
import net.carboninter.actors.TwitterTermsActor
import net.carboninter.actors.TwitterTermsActor.{GetState, SetState}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.metrics.Metrics
import net.carboninter.models.{StubTweet, TwitterTermsCommand}
import net.carboninter.util.Logging
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class TwitterService(config: Config)(implicit actorSystem: ActorSystem) extends Logging {

  import actorSystem.dispatcher
  implicit val askTimeout = Timeout(5.seconds)

  val twitterConnector = new TwitterConnector(config)

  val ttaRef: ActorRef = actorSystem.actorOf(Props(new TwitterTermsActor))

  val setTwitterTermsStateFlow = Flow[TwitterTermsCommand]
    .map { command =>
      SetState(command.liveTracks.map(_.track).sorted)
    }
    .ask[List[String]](ttaRef)

  val tweetStream = Flow[NotUsed]
    .flatMapConcat { _ =>
      tweetSource
      .map { case (js, liveTerms) =>

        val lowerText = js.as[StubTweet].noNewLines.text.toLowerCase

        //We found a tweet containing a track name, log metric to see which tracks are found most often
        for(track <- liveTerms if lowerText.contains(track)) {
          Metrics.tweetTrackMentionCounter.labels(track).inc()
        }

        (js, liveTerms, lowerText)
      }
    }

  def tweetSource = {

    trait StreamMsg {
      def liveTerms: List[String]
    }
    case class JsonMsg(jsObject: JsObject, liveTerms: List[String]) extends StreamMsg
    case class HeartBeatMsg(liveTerms: List[String]) extends StreamMsg

    def currentTerms = (ttaRef ? GetState("TweetStreamBuilder")).map(_.asInstanceOf[List[String]])

    val heartBeat = Source.tick(1.second, 5.second, ByteString("hb\n"))

    RestartSource.onFailuresWithBackoff(RestartSettings(2.seconds, 5.minutes, 0.0)) { () =>
      Source.futureSource {
        currentTerms map {

          case Nil => Source.empty
          case streamTerms =>

            logger.info("Twitter stream starting with the terms: " + streamTerms.mkString(", "))

            val tweetSource = twitterConnector.buildTweetStreamAkka(streamTerms)

            Source.combine(heartBeat, tweetSource)(Merge(_, eagerComplete = true))
              .via(Framing.delimiter(ByteString.fromString("\n"), 40000, allowTruncation = true))
              .zip(Source.repeat(GetState("RunningTweetStream")).ask[List[String]](ttaRef))
              .mapConcat { case (bs, liveTerms) =>
                bs.utf8String match {
                  case s if s.forall(_.isWhitespace) => // its 0xd \r
                    Metrics.twitterKeepAliveCrCounter.inc()
                    None
                  case "hb" =>
                    Some(HeartBeatMsg(liveTerms))
                  case s =>
                    Try(Json.parse(s).as[JsObject]) match {
                      case Failure(exception) =>
                        logger.error("Error parsing tweet json", exception)
                        logger.error(s)
                        Metrics.unparsableFrameCounter.labels("unknown").inc() //TODO - try to put a reason in here
                        None
                      case Success(jsObject) =>
                        Metrics.tweetParsedCounter.inc()
                        Some(JsonMsg(jsObject, liveTerms))
                    }
                }
              }
              .takeWhile {
                case msg if msg.liveTerms != streamTerms =>
                  logger.info("Twitter stream ending, these terms old: " + streamTerms.mkString(", "))
                  false
                case _ => true
              }
              .collect {
                case JsonMsg(jsObject, liveTerms) => (jsObject, liveTerms)
              }
        }
      }
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
