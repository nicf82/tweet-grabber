package net.carboninter.connectors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{KillSwitches, SharedKillSwitch, UniqueKillSwitch}
import akka.stream.scaladsl.{Framing, Keep, Source}
import akka.util.ByteString
import com.typesafe.config.Config
import net.carboninter.util.Logging
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.StandaloneWSRequest
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import java.net.URLEncoder
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class TwitterConnector(config: Config)(implicit actorSystem: ActorSystem) extends Logging {

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

  def tweetSource(ks: SharedKillSwitch, phrase: String): Source[JsValue, SharedKillSwitch] = Source.futureSource(wsClient
      .url("https://stream.twitter.com/1.1/statuses/filter.json?track=" + URLEncoder.encode(phrase, "UTF-8"))
      .sign(OAuthCalculator(consumerKey, requestToken))
      .withMethod("GET")
      .stream()
      .map(_.bodyAsSource)
    )
    .viaMat(ks.flow)(Keep.right)
    .viaMat(Framing.delimiter(ByteString.fromString("\n"), 20000))(Keep.left)
    .mapConcat { bs =>
      Try(Json.parse(bs.utf8String)) match {
        case Failure(exception) =>
          logger.error("Error parsing tweet json" + exception)
          None
        case Success(value) =>
          Some(value)
      }
    }

  def getRetweetsSource(id: String): Source[JsObject, Future[NotUsed]] =
    Source.futureSource(getRetweets(id).map(x => Source( x.toSeq)))
}
