package net.carboninter.connectors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.pattern.CircuitBreaker
import akka.stream.{KillSwitches, SharedKillSwitch, UniqueKillSwitch}
import akka.stream.scaladsl.{Framing, Keep, Source}
import akka.util.ByteString
import com.typesafe.config.Config
import net.carboninter.metrics
import net.carboninter.metrics.Metrics
import net.carboninter.util.Logging
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.{StandaloneWSRequest, StandaloneWSResponse}
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import java.net.URLEncoder
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

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

  def getRetweetsSource(id: String): Source[JsObject, Future[NotUsed]] =
    Source.futureSource(getRetweets(id).map(x => Source( x.toSeq)))

  def tweetSource(phrase: String): Source[JsObject, NotUsed] = Source.futureSource(getTweetStream(phrase))
    .viaMat(Framing.delimiter(ByteString.fromString("\n"), 20000))(Keep.right)
    .mapConcat { bs =>
      Try(Json.parse(bs.utf8String).as[JsObject]) match {
        case Failure(exception) =>
          logger.error("Error parsing tweet json ", exception)
          logger.error(bs.utf8String)
          Metrics.unparsableFrameCounter.labels("unknown").inc()  //TODO - try to put a reason in here
          None
        case Success(value) =>
          Metrics.tweetParsedCounter.inc()
          Some(value)
      }
    }

  //TODO - maybe replace with a RestartSource.withBackoff
  val cb = new CircuitBreaker(
    scheduler = actorSystem.scheduler,
    maxFailures = 2,
    callTimeout = 20.seconds,
    resetTimeout = 1.second,
    maxResetTimeout = 1.minute,
    exponentialBackoffFactor = 2.0
  )

  //TODO - add metrics around this stuff
  cb.onCallSuccess(t => logger.info(s"Twitter stream connection established, took ${t/1000}ms"))
  cb.onCallFailure(t => logger.warn(s"Twitter stream connection could not be established, not due to timeout after ${t/1000}ms"))
  cb.onCallTimeout(t => logger.warn(s"Twitter stream connection could not be established due to timeout after ${t/1000}ms"))
  cb.onOpen(logger.warn("Twitter stream circuit breaker open"))
  cb.onHalfOpen(logger.warn("Twitter stream circuit breaker half open"))
  cb.onClose(logger.warn("Twitter stream circuit breaker closed"))
  cb.onCallBreakerOpen(logger.warn("Twitter stream connection call failed due to circuit breaker being open"))

  private def getTweetStream(phrase: String): Future[Source[ByteString, _]] = {

    def theCall: Future[StandaloneWSResponse] = wsClient
      .url("https://stream.twitter.com/1.1/statuses/filter.json?track=" + URLEncoder.encode(phrase, "UTF-8"))
      .sign(OAuthCalculator(consumerKey, requestToken))
      .withMethod("GET")
      .stream()

    val failureFn = (x: Try[StandaloneWSResponse]) => x match {
      case Success(response) => response.status != 200
      case _                 => false
    }

    cb.withCircuitBreaker(theCall, failureFn)
      .map { response =>
        Metrics.streamConnectResponseCounter.labels(response.status.toString).inc()
        response
      }
      .map(_.bodyAsSource)
      .recover {
        case e =>
          logger.error("Error connecting twitter stream", e)
          Metrics.streamConnectResponseCounter.labels(e.getClass.getName).inc()
          Source.empty[ByteString]
      }
  }
}
