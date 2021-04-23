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

    def currentTerms = (ttaRef ? GetState("TweetStreamBuilder")).map(_.asInstanceOf[List[String]])

    val heartBeat = Source.tick(1.second, 5.second, ByteString.empty)

    RestartSource.onFailuresWithBackoff(RestartSettings(2.seconds, 5.minutes, 0.0)) { () =>
      Source.futureSource {
        currentTerms map {

          case Nil => Source.empty
          case streamTerms =>

            logger.info("Twitter stream starting with the terms: " + streamTerms.mkString(", "))

            val tweetSource = twitterConnector.buildTweetStreamAkka(streamTerms)

            Source.combine(heartBeat, tweetSource)(Merge(_, eagerComplete = true))
              .zip(Source.repeat(GetState("RunningTweetStream")).ask[List[String]](ttaRef))
              .takeWhile {
                case (_, liveTerms) if liveTerms != streamTerms =>
                  logger.info("Twitter stream ending, these terms old: " + streamTerms.mkString(", "))
                  false
                case _ => true
              }
              .mapConcat {
                case (bs, _) if !bs.isEmpty => Some(bs)
                case _ => None
              }
              .viaMat(Framing.delimiter(ByteString.fromString("\n"), 40000, allowTruncation = true))(Keep.right)
              .mapConcat { bs =>

                val s = bs.utf8String
                if(s.forall(_.isWhitespace)) {
                  Metrics.twitterKeepAliveCrCounter.inc()
                  logger.info("Skipping keepalive: " + s.map(_.toInt.formatted("0x%x")).mkString(", "))
                  None
                }
                else {
                  Try(Json.parse(s).as[JsObject]) match {
                    case Failure(exception) =>
                      logger.error("Error parsing tweet json", exception)
                      logger.error(s)
                      Metrics.unparsableFrameCounter.labels("unknown").inc() //TODO - try to put a reason in here
                      None
                    case Success(value) =>
                      Metrics.tweetParsedCounter.inc()
                      Some((value, streamTerms))
                  }
                }
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
