package net.carboninter.services

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt.scaladsl._
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl._
import akka.util.ByteString
import com.typesafe.config.Config
import net.carboninter.models.TwitterTermsCommand
import net.carboninter.util.{JsonDecoder, Logging}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

import scala.concurrent.Future
import scala.util.{Failure, Random, Success, Try}

class MqttService(config: Config)(implicit actorSystem: ActorSystem) extends JsonDecoder with Logging {

  import actorSystem.dispatcher

  val clientID: String = config.getString("mqtt.clientId") + "-" + Random.alphanumeric.take(6).mkString
  val subscribeTopic = "tweet-grabber/to/set-terms"
  val publishTopic = "tweet-grabber/from/tweet"

  val connectionSettings = MqttConnectionSettings(
    config.getString("mqtt.uri"),
    clientID,
    new MemoryPersistence
  ).withAutomaticReconnect(true)

  val twitterTermsCommandSource: Source[TwitterTermsCommand, Future[Done]] =
    MqttSource.atLeastOnce(
      connectionSettings
        .withCleanSession(false)
        .withClientId(clientID+"-1"),
      MqttSubscriptions(subscribeTopic, MqttQoS.AtLeastOnce),
      bufferSize = 1024
    )
    .mapAsync(1) { msgWithAck =>
      msgWithAck.ack().map(_ => msgWithAck.message)
    }
    .mapConcat { msg =>
      val jsonString = msg.payload.utf8String
      logger.info("Received message: " + jsonString)
      decodeAs[TwitterTermsCommand](jsonString)
    }

  val publishTweetSink = Flow[JsObject]
    .map { entryResult =>
      MqttMessage(publishTopic, ByteString(entryResult.toString))
    }
    .to(MqttSink(connectionSettings, MqttQoS.AtLeastOnce))

}
