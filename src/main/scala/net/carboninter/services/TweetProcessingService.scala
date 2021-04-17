package net.carboninter.services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import net.carboninter.metrics.Metrics
import net.carboninter.models.{Entry, StubTweet}
import net.carboninter.repos.{EntryRepo, NewTweetRepo, TweetRepo, TweetStubRepo}
import net.carboninter.util.{Logging, Text}
import net.carboninter.util.Text.hilight
import play.api.libs.json.{JsError, JsObject, JsString, JsSuccess, Json, __}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TweetProcessingService(
  tweetRepo: TweetRepo,
  newTweetRepo: NewTweetRepo,
  entryRepo: EntryRepo,
  tweetStubRepo: TweetStubRepo
)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) extends Logging {

  val standardIncomingTweetFlow = Flow[JsObject].mapConcat { jsTweet =>
      logger.trace("Incoming tweet: " + jsTweet)
      Metrics.processedTweetsCounter.inc()

      //Grab an abbreviated tweet - we can store this for all tweets, for future reference in case we decide to grab the whole tweet
      jsTweet.validate[StubTweet] match {
        case JsSuccess(stubTweet, _) =>
          logger.debug("Parsed stub tweet: " + stubTweet.noNewLines)
          Some((stubTweet, jsTweet))
        case JsError(errors) =>
          logger.error("Failed to parse stub tweet! The tweet must be very corrupt, it's been logged below as a last resort. Errors: " + errors)
          logger.error(jsTweet.toString)
          Metrics.stubTweetParseFailureCounter.inc()
          None
      }
    }
    .mapAsync(8) { case (stubTweet, jsTweet) =>
      tweetStubRepo.store(stubTweet).map(wr => (wr, stubTweet, jsTweet))
    }
    .map { case (wr, stubTweet, jsTweet) =>
      logger.debug(s"${wr.n} tweet stubs stored for ${stubTweet.id_str}")
      Metrics.storedStubTweetsCounter.inc(wr.n)
      (stubTweet, jsTweet)
    }

  val tweetEventMatchingFlow = Flow[(StubTweet,JsObject)].mapConcat { case (stubTweet, jsTweet) =>

      println((jsTweet \ "lang").as[JsString].value)

      (jsTweet \ "lang").as[JsString].value match {
        case "en" => Some( (stubTweet, jsTweet) )
        case _    => None
      }
    }
    .flatMapConcat { case (stubTweet, jsTweet) =>
      //Check if the tweet may be related to a live event (at time of tweet)

      val minFirstAppearance = stubTweet.time
      val maxMarketStartTime = stubTweet.time.minusHours(24)

      entryRepo.getEntriesStream(minFirstAppearance, maxMarketStartTime).map { entry =>
        (entry, stubTweet, jsTweet)
      }
    }
    .mapConcat {
      case (Success(entry), stubTweet, jsTweet) =>
        logger.trace(s"Found entry: firstAppearance: ${entry.firstAppearance} - start: ${entry.marketStartTime} - cutoff: ${entry.marketStartTime.plusHours(24)} live at time of tweet ${stubTweet.time}, info: ${entry._id}")
        Metrics.entriesReturnedCounter.inc()
        val lowerText = stubTweet.noNewLines.text.toLowerCase

        //Check if both track and horse were found
        if( Text.matchEntryToTweet(entry.name, entry.track, lowerText) ) {
          logger.debug("Matched tweet text: " + hilight(lowerText, entry.name, entry.track))
          Metrics.entriesMatchedCounter.inc()
          Some( (Some(entry), stubTweet, jsTweet) )
        }
        else None

      case (Failure(e), stubTweet, jsTweet) =>
        logger.error(s"Failed to parse entry! Tweet ${stubTweet.id_str} MAY NOT have been assigned to an entry it should have been. The tweet stub's entryErrors will be incremented, for reprocessing", e)
        Metrics.entryParseFailureCounter.inc()
        Some( (None, stubTweet, jsTweet) )
    }
    .statefulMapConcat { () =>
      var lastTweetId = ""

      {
        // We only store the tweet when a new tweet is is seen, then continue as normal
        case (entry, stubTweet, jsTweet) if stubTweet.id_str != lastTweetId =>
          lastTweetId = stubTweet.id_str

          val r = newTweetRepo.store(stubTweet.id_str, jsTweet) map { wr =>
            logger.debug(s"${wr.n} full tweet records stored for ${stubTweet.id_str}")
            Metrics.storedFullTweetsCounter.inc()
            (entry, stubTweet)
          }
          Some(r)
        //Not a new tweet
        case  (entry, stubTweet, _) =>
          Some(Future.successful((entry, stubTweet)))
      }
    }
    .mapAsync(8)(identity)
    .mapConcat {
      case (Some(entry), stubTweet) =>
        Some((entry, stubTweet))
      case (None, stubTweet) =>
        logger.debug(s"Incrementing entryErrors on tweet stub ${stubTweet.id_str}")
        tweetStubRepo.incEntryErrors(stubTweet.id_str)
        None
    }
    .mapAsync(8) { case (entry, stubTweet) =>
      entryRepo.attachTweet(entry._id, stubTweet.id_str).map(wr => (wr, entry, stubTweet))
    }
    .map { case (wr, entry, stubTweet) =>
      logger.debug(s"${wr.n} records updated attaching tweet ${stubTweet.id_str} to entry ${entry._id}")
      Metrics.tweetsAttachedCounter.inc(wr.n)
      stubTweet
    }

  val batch = 2

  val markTweetProcessedFlow = Flow[(StubTweet,JsObject)]
    .mapAsync(8) { case (stubTweet, jsTweet) =>
      tweetRepo.setBatchIdent(stubTweet.id_str, batch).map(wr => (wr, stubTweet, jsTweet))
    }
    .map { case (wr, stubTweet, jsTweet) =>
      logger.debug(s"${wr.n} tweet ${stubTweet.id_str} batch set to: $batch")
      (stubTweet, jsTweet)
    }



}