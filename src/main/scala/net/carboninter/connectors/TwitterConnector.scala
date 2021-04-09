package net.carboninter.connectors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import play.api.libs.json.{JsArray, JsObject, JsValue}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Future

class TwitterConnector(config: Config)(implicit actorSystem: ActorSystem) {

  import actorSystem.dispatcher

  val wsClient = StandaloneAhcWSClient()

  val apiKey = config.getString("twitter.apiKey")
  val apiSecret = config.getString("twitter.apiSecret")
  val token = config.getString("twitter.token")
  val tokenSecret = config.getString("twitter.tokenSecret")

  val consumerKey = ConsumerKey(apiKey, apiSecret)
  val requestToken = RequestToken(token, tokenSecret)

  def getTweet(id: String): Future[JsObject] = {
    wsClient
      .url(s"https://api.twitter.com/1.1/statuses/show.json?id=$id")
      .sign(OAuthCalculator(consumerKey, requestToken))
      .get()
      .map { response =>
        response.body[JsValue].as[JsObject]
      }
  }

  def getRetweets(id: String): Future[collection.IndexedSeq[JsObject]] = {
    wsClient
      .url(s"https://api.twitter.com/1.1/statuses/retweets/$id.json")
      .sign(OAuthCalculator(consumerKey, requestToken))
      .get()
      .map { response =>
        response.body[JsValue].as[JsArray].value.map(_.as[JsObject])
      }
  }

  def getRetweetsSource(id: String): Source[JsObject, Future[NotUsed]] =
    Source.futureSource(getRetweets(id).map(x => Source( x.toSeq)))
}
