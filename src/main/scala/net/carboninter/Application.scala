package net.carboninter

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.repos.TweetRepo
import net.carboninter.services.TwitterService
import net.carboninter.util.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import reactivemongo.api.AsyncDriver

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object Application extends App with Logging {

  val config: Config = ConfigFactory.load()
  val driver = new AsyncDriver()

  // Create Akka system for thread and streaming management
  implicit val system = ActorSystem()
  import system.dispatcher

  system.registerOnTermination {
    System.exit(0)
  }

  implicit val materializer = SystemMaterializer(system).materializer

  val service = new TwitterService(config)

  val repo = new TweetRepo(driver, config)

  repo.tweetsSource()



}
