package net.carboninter

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.connectors.TwitterConnector
import net.carboninter.repos.{EntryRepo, TweetRepo, TweetStubRepo}
import net.carboninter.services.TweetProcessingService
import net.carboninter.util.Logging
import reactivemongo.api.AsyncDriver

import java.time.OffsetDateTime

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

  val connector = new TwitterConnector(config)

  val tweetRepo = new TweetRepo(driver, config)
  val tweetStubRepo = new TweetStubRepo(driver, config)
  val entryRepo = new EntryRepo(driver, config)


  val service = new TweetProcessingService(config, tweetRepo, entryRepo, tweetStubRepo)

  val r = service.tempSingleTweetSource.via(service.tweetProcessingFlow).runWith(Sink.fold(0) { case (acc, e) => acc+1 })

  r map { i =>
    logger.info(s"${i} messages processed")
    actorSystem.terminate()
  }

//  entryRepo.getEntries(OffsetDateTime.parse("2021-04-01T11:17:49.715Z"), OffsetDateTime.parse("2021-04-03T15:46:00.001Z")) map { entries =>
//
//    for(entry <- entries) {
//      println(entry)
//    }
//    println(entries.length)
//
//  } recover {
//    case e: Throwable =>
//      logger.error("Error", e)
//  }

//  entryRepo.getEntriesSurroundingTweetTime(OffsetDateTime.parse("1970-01-01T00:00:00.000Z")) map { entries =>  //Gives results, fix!
//  entryRepo.getEntriesSurroundingTweetTime(OffsetDateTime.parse("2020-07-16T12:59:43.137Z")) map { entries =>
//    for(entry <- entries) {
//      println(entry)
//    }
//    println(entries.length)
//
//    entries.head.validate[LiveEvent] match {
//      case JsSuccess(value, path) =>
//        println(value)
//      case JsError(errors) =>
//        println(errors)
//    }
//  }






}
