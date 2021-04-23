package net.carboninter.connectors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, headers}
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import net.carboninter.metrics.Metrics
import net.carboninter.util.{Logging, TwitterOAuthHeaderGenerator}
import play.api.libs.json.{JsArray, JsObject, JsValue}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.{AhcWSClientConfigFactory, StandaloneAhcWSClient}
import play.shaded.ahc.org.asynchttpclient.Param
import play.shaded.ahc.org.asynchttpclient.oauth.OAuthSignatureCalculatorInstance

import java.net.URLEncoder
import java.util
import scala.concurrent.Future
import scala.jdk.CollectionConverters._


class TwitterConnector(config: Config)(implicit actorSystem: ActorSystem) extends Logging {

  import actorSystem.dispatcher

  val wsClient = StandaloneAhcWSClient(AhcWSClientConfigFactory.forClientConfig())

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

  val headerGenerator = new TwitterOAuthHeaderGenerator(apiKey, apiSecret, token, tokenSecret)

  //See https://developer.twitter.com/en/docs/twitter-api/v1/tweets/filter-realtime/guides/basic-stream-parameters
  def buildTweetStreamAkka(terms: List[String]) = {

    val baseUrl = "https://stream.twitter.com/1.1/statuses/filter.json"
    val params = Map(
      "language" -> "en",
      "track" -> terms.mkString(",")
    )

    val authToken = headerGenerator.generateHeader("GET", baseUrl, params.asJava)
    val authHeader = headers.RawHeader("authorization", authToken)
    val url = baseUrl + "?" + params.map(kv => kv._1+"="+URLEncoder.encode(kv._2, "UTF-8")).mkString("&")

    val request = HttpRequest(HttpMethods.GET, url, List(authHeader))

    val fs = for {
      response <- Http()(actorSystem).singleRequest(request)
    } yield {
      Metrics.streamConnectResponseCounter.labels(response.status.toString).inc()
      response.status match {
        case code if code.isSuccess() =>
          response.entity.dataBytes
        case code =>
          response.discardEntityBytes()
          throw new RuntimeException(s"Twitter Stream connect failed with status: $code")

      }
    }

    Source.futureSource(fs)
  }

}
