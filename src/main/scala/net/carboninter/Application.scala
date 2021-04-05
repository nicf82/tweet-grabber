package net.carboninter

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.util.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object Application extends App with Logging {

  val config: Config = ConfigFactory.load()

  // Create Akka system for thread and streaming management
  implicit val system = ActorSystem()
  import system.dispatcher

  system.registerOnTermination {
    System.exit(0)
  }

  implicit val materializer = SystemMaterializer(system).materializer

  val service = new TwitterService(config)

  service.getTweet("1282680856289697794") map { tweet =>
    println(Json.prettyPrint(tweet))
  }



}
