package net.carboninter.connectors

import com.typesafe.config.{Config, ConfigFactory}
import net.carboninter.testutil.TestActorSystem
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json




class TwitterConnectorSpec extends AnyWordSpecLike with TestActorSystem {

  val config: Config = ConfigFactory.load()
  val service = new TwitterConnector(config)

  import system.dispatcher

  "Grabbing different retweets" should {

    val config: Config = ConfigFactory.load()

    val service = new TwitterConnector(config)

    "grab a tweet's retweets" ignore {

      service.getRetweets("1195716311726395392") map { retweets =>
        for(tweet <- retweets) {
          println(Json.prettyPrint(tweet))
        }
        println(s"${retweets.size} records")
      }
    }

  }

  "Grabbing different types of tweet" should {

    val config: Config = ConfigFactory.load()

    val service = new TwitterConnector(config)

    "grab a normal tweet" ignore {

      service.getTweet("1241831632249724934") map { tweet =>

        (tweet \ "retweeted_status").toOption.isDefined shouldBe false
        (tweet \ "quoted_status").toOption.isDefined shouldBe false

        //println(Json.prettyPrint(tweet))
      }
    }

    "grab a tweet that quotes the one above" ignore {

      service.getTweet("1242347751293292544") map { tweet =>

        (tweet \ "retweeted_status").toOption.isDefined shouldBe false
        (tweet \ "quoted_status").toOption.isDefined shouldBe true

        //println(Json.prettyPrint(tweet))
      }
    }
  }
}
