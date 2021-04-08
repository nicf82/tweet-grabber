package net.carboninter.services

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import com.typesafe.config.Config
import net.carboninter.repos.{EntryRepo, TweetRepo, TweetStubRepo}
import net.carboninter.util.Logging
import play.api.libs.json.{JsObject, JsString}

import java.time.{Instant, ZoneOffset}
import scala.concurrent.ExecutionContext
import scala.util.Success

class TweetProcessingService(
  config: Config,
  tweetRepo: TweetRepo,
  entryRepo: EntryRepo,
  tweetStubRepo: TweetStubRepo
)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer) extends Logging {

  val tempSingleTweetSource = tweetRepo.tweetsSource(3).drop(2000).take(1)

  val tweetProcessingFlow = Flow[JsObject].map { json =>
      logger.debug("Read tweet: " + json)
      //Grab the id and the text - we can store this for all tweets, for future reference in case we decide to grab the whole tweet
      val id = (json \ "id_str").as[JsString].value
      val text = (json \ "extended_tweet").asOpt[JsObject].fold[String]((json \ "text").as[JsString].value)(j => (j \ "text").as[JsString].value)
      (id, text, json)
    }
    .mapAsync(4) { case (id, text, json) =>
      tweetStubRepo.store(id, text).map(wr => (wr, id, json))
    }
    .map { case (wr, id, json) =>
      logger.debug(s"${wr.n} tweet stubs stored for $id")
      (id, json)
    }
    .flatMapConcat { case (id, json) =>
      //Check if the tweet may be related to a live event (at time of tweet)
      val timestampMs = (json \ "timestamp_ms").as[JsString].value.toLong
      val tweetTime = Instant.ofEpochMilli(timestampMs).atOffset(ZoneOffset.UTC)
      val minFirstAppearance = tweetTime
      val maxMarketStartTime = tweetTime.minusHours(24)


      val future = entryRepo.getEntries(minFirstAppearance, maxMarketStartTime).map(_.zip(LazyList.continually((id, json, tweetTime))))
      Source.futureSource(future.map(Source.apply))
    }
    .map {
      case (Success(entry), (id, json, tweetTime)) =>
        logger.info(s"Found entry: ${entry.firstAppearance} - ${entry.marketStartTime} live at time of tweet $tweetTime, info: ${entry._id}")
    }



}
