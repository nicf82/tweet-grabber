package net.carboninter

import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, Supervision, SystemMaterializer}
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.repos.{DbProvider, EntryRepo, NewTweetRepo, TweetRepo, TweetStubRepo}
import net.carboninter.services.TweetProcessingService
import net.carboninter.util.Logging
import reactivemongo.api.AsyncDriver

//See https://doc.akka.io/docs/alpakka/1.1.2/examples/mqtt-samples.html

object Application extends App with Logging {

  val config: Config = ConfigFactory.load()
  val driver = new AsyncDriver()

  // Create Akka system for thread and streaming management
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val materializer = SystemMaterializer(actorSystem).materializer

  actorSystem.registerOnTermination {
    System.exit(0)
  }

  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

  val connector = new TwitterConnector(config)

  val dbProvider = new DbProvider(driver, config)

  val tweetRepo = new TweetRepo(dbProvider)
  val newTweetRepo = new NewTweetRepo(dbProvider)
  val tweetStubRepo = new TweetStubRepo(dbProvider)
  val entryRepo = new EntryRepo(dbProvider)

  val service = new TweetProcessingService(tweetRepo, newTweetRepo, entryRepo, tweetStubRepo)

  val r = service.tempSingleTweetSource1
    .via(service.standardIncomingTweetFlow)
    .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
    .runWith(Sink.fold(0) { case (acc, _) => acc+1 })

  r map { i =>
    logger.info(s"${i} messages processed")
    actorSystem.terminate()
  }
}
