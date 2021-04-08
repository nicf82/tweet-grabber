package net.carboninter.flows

import akka.stream.scaladsl.Flow
import net.carboninter.Application.logger
import net.carboninter.models.DissectedTweet
import play.api.libs.json.JsObject

object Flows {

  val dissectTweetFlow = Flow[JsObject]
    .map { json =>
      DissectedTweet(
        json,
        (json \ "retweeted_status").toOption.map(_.as[JsObject]),
        (json \ "quoted_status").toOption.map(_.as[JsObject])
      )
    }
    .map { case dissectedTweet =>

      val retweeting = dissectedTweet.retweetedTweet.map { innerJson =>
        logger.debug("Found a retweet! Original: " + dissectedTweet.tweet)
        logger.debug("Retweeted                : " + innerJson)
        innerJson
      }

      val quoting = dissectedTweet.quotedTweet.map { innerJson =>
        logger.debug("Found a quoted tweet! Original: " + dissectedTweet.tweet)
        logger.debug("Quoted                        : " + innerJson)
        innerJson
      }



      if(dissectedTweet.retweetsAnotherTweet && dissectedTweet.quotesAnotherTweet) {
        logger.warn("Both!!!")
      }

      dissectedTweet
    }
}
