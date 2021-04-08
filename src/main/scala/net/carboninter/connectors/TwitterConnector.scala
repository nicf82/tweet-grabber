package net.carboninter.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

class TwitterConnector(config: Config)(implicit actorSystem: ActorSystem) {

  import actorSystem.dispatcher

  val wsClient = StandaloneAhcWSClient()

  val apiKey = config.getString("twitter.apiKey")
  val apiSecret = config.getString("twitter.apiSecret")
  val token = config.getString("twitter.token")
  val tokenSecret = config.getString("twitter.tokenSecret")

  val consumerKey = ConsumerKey(apiKey, apiSecret)
  val requestToken = RequestToken(token, tokenSecret)

  def getTweet(id: String) = {
    wsClient
      .url(s"https://api.twitter.com/1.1/statuses/show.json?id=$id")
      .sign(OAuthCalculator(consumerKey, requestToken))
      .get()
      .map { response =>
        response.body[JsValue].as[JsObject]
      }
  }

  //
  //
  //  //This is the timestamp 1 week before the last tweet which is a retweet, so
  //  // we should cover all retweets when analysing their source tweets
  //  val startAt = "1594045933685"
  //
  //  val currentBatchIdent = 3
  //
  //  tweetRepo.count(currentBatchIdent) map { count =>
  //    println("Tweets to process: " + count)
  //  }
  //

  //
  //
  //
  //  val result = tweetRepo.tweetsSource(currentBatchIdent)
  //    .drop(100000)
  //    .take(1000)
  //    .via(Flows.dissectTweetFlow)
  //    .toMat(Sink.fold((0,0,0,0)) { case ((r, q, b, t), dissectedTweet) =>
  //      val r1 = r + (if(dissectedTweet.retweetsAnotherTweet) 1 else 0)
  //      val q1 = q + (if(dissectedTweet.quotesAnotherTweet) 1 else 0)
  //      val b1 = b + (if(dissectedTweet.quotesAnotherTweet && dissectedTweet.retweetsAnotherTweet) 1 else 0)
  //
  //      (r1, q1, b1, t+1)
  //    })(Keep.right)
  //    .run
  //    .map { case (r, q, b, t) =>
  //      println(s"Total: $t, retweets: $r, quoted: $q, both: $b")
  //    }
}
