package net.carboninter.services

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import net.carboninter.metrics.Metrics
import net.carboninter.models.StubTweet
import net.carboninter.repos.{EntryRepo, NewTweetRepo, TweetRepo, TweetStubRepo}
import net.carboninter.util.Logging
import net.carboninter.util.Text.hilight
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TweetProcessingService(
  tweetRepo: TweetRepo,
  newTweetRepo: NewTweetRepo,
  entryRepo: EntryRepo,
  tweetStubRepo: TweetStubRepo
)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) extends Logging {

  val tempSingleTweetSource1 = Source.futureSource(tweetRepo.get("1195770077150363649").map(t => Source.single(t.get)))
  val tempSingleTweetSource2 = Source.futureSource(tweetRepo.get("1195704270743654400").map(t => Source.single(t.get)))

  val standardIncomingTweetFlow = Flow[JsObject].mapConcat { jsTweet =>
      logger.trace("Incoming tweet: " + jsTweet)
      Metrics.incomingTweetsCounter.inc()

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
    .mapAsync(4) { case (stubTweet, jsTweet) =>
      tweetStubRepo.store(stubTweet).map(wr => (wr, stubTweet, jsTweet))
    }
    .map { case (wr, stubTweet, jsTweet) =>
      logger.debug(s"${wr.n} tweet stubs stored for ${stubTweet.id_str}")
      Metrics.storedStubTweetsCounter.inc(wr.n)
      (stubTweet, jsTweet)
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

        if(lowerText.contains(entry.name) && lowerText.contains(entry.track)) {
          logger.debug("Matched tweet text: " + hilight(lowerText, entry.name, entry.track))
          Metrics.entriesMatchedCounter.inc()
          Some( (Some(entry), stubTweet, jsTweet) )
        }
        else None

      case (Failure(e), stubTweet, jsTweet) =>
        logger.error(s"Failed to parse entry! Tweet ${stubTweet.id_str} MAY not have been assigned to an entry in error. The tweet will be stored with entryError flag set, for reprocessing and possible removal.", e)
        Metrics.entryParseFailureCounter.inc()
        Some( (None, stubTweet, jsTweet) )
    }
    .statefulMapConcat { () =>
      var lastTweetId = ""

      {
        // We only store the tweet when a new tweet is is seen, then continue as normal
        case (entry, stubTweet, jsTweet) if stubTweet.id_str != lastTweetId =>
          lastTweetId = stubTweet.id_str

          val js = if(!entry.isDefined) jsTweet else jsTweet ++ Json.obj("entryError" -> true)
          val r = newTweetRepo.store(stubTweet.id_str, js) map { wr =>
            logger.debug(s"${wr.n} full tweet records stored for ${stubTweet.id_str}, entryError = ${!entry.isDefined}")
            Metrics.storedFullTweetsCounter.inc()
            (entry, stubTweet)
          }
          Some(r)
        //Not a new tweet
        case  (entry, stubTweet, _) =>
          Some(Future.successful((entry, stubTweet)))
      }
    }
    .mapAsync(1)(identity)
    .mapConcat { case (entry, stubTweet) =>
      entry.map(e => (e, stubTweet))
    }
    .mapAsync(4) { case (entry, stubTweet) =>
      entryRepo.attachTweet(entry._id, stubTweet.id_str).map(wr => (wr, entry, stubTweet))
    }
    .map { case (wr, entry, stubTweet) =>
      logger.debug(s"${wr.n} records updated attaching tweet ${stubTweet.id_str} to entry ${entry._id}")
      Metrics.tweetsAttachedCounter.inc(wr.n)
    }

}
