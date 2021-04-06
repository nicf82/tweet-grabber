package net.carboninter

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.repos.TweetRepo
import net.carboninter.services.TwitterService
import net.carboninter.util.Logging
import play.api.libs.json.{JsDefined, JsObject, JsUndefined}
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


  //Checking how many retweets there are
  //  db.getCollection('tweets').aggregate([
  //  {'$group': {
  //    '_id':{'$gt':["$retweeted_status", null]},
  //    'count': {'$sum': 1}
  //  }}
  //  ])

  //A tweet with 6 retweets
  //  db.getCollection('tweets').find({'retweeted_status.id_str': '1201124908979359744'})


  //This is the timestamp 1 week before the last tweet which is a retweet, so
  // we should cover all retweets when analysing their source tweets
  val startAt = "1594045933685"

  val currentBatchIdent = 3

  repo.count(currentBatchIdent) map { count =>
    println("Tweets to process: " + count)
  }


  val result = repo.tweetsSource(currentBatchIdent)
    .drop(20000)
    .take(10)
    .mapConcat { originalJson =>

      val retweeting = (originalJson \ "retweeted_status").toOption.toList.flatMap {
        case innerJson: JsObject =>
          logger.debug("Found a retweet! Original: " + originalJson)
          List(innerJson)
      }

      val quoting = (originalJson \ "quoted_status").toOption.toList.flatMap {
        case innerJson: JsObject =>
          logger.debug("Found a quoted tweet! Original: " + originalJson)
          List(originalJson, innerJson)
      }

      retweeting ++ quoting
    }
    .to(Sink.foreach(x => println(x)))
    .run

}
