package net.carboninter

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.connectors.TwitterConnector
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike

trait TestActorSystem {

  implicit val system = ActorSystem()

  system.registerOnTermination {
    System.exit(0)
  }

  //implicit val materializer = SystemMaterializer(system).materializer
}

class ApplicationTest extends AnyWordSpecLike with TestActorSystem {

  val config: Config = ConfigFactory.load()
  val service = new TwitterConnector(config)

  import system.dispatcher

  "Grabbing different types of tweet" should {

    val config: Config = ConfigFactory.load()

    val service = new TwitterConnector(config)

    "grab a normal tweet" in {

      service.getTweet("1241831632249724934") map { tweet =>

        (tweet \ "retweeted_status").toOption.isDefined shouldBe false
        (tweet \ "quoted_status").toOption.isDefined shouldBe false

        //println(Json.prettyPrint(tweet))
      }
    }

    "grab a tweet that quotes the one above" in {

      service.getTweet("1242347751293292544") map { tweet =>

        (tweet \ "retweeted_status").toOption.isDefined shouldBe false
        (tweet \ "quoted_status").toOption.isDefined shouldBe true

        //println(Json.prettyPrint(tweet))
      }
    }
  }
}
