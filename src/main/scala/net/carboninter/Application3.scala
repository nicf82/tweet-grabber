package net.carboninter

import akka.actor.ActorSystem
import akka.stream.{KillSwitches, SharedKillSwitch, SystemMaterializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._


// Interesting
// http://beyondthelines.net/computing/akka-streams-patterns/

object Application3 extends App {


  val config: Config = ConfigFactory.load()

  // Create Akka system for thread and streaming management
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  implicit val materializer = SystemMaterializer(actorSystem).materializer

  var killSwitch: Option[SharedKillSwitch] = None

  val res = Source(Seq("one", "two", "three", "four", "five")).throttle(1, 1.second)
    .flatMapMerge(2, cmd => {
      killSwitch.map(_.shutdown())
      killSwitch  = Some(KillSwitches.shared("killswitch"))
      val ((nu, ks), res1) = Source(Seq(cmd + "1", cmd + "2", cmd + "3", cmd + "4", cmd + "5", cmd + "6")).throttle(1, 900.millis)
        .viaMat(KillSwitches.single)(Keep.both)
        .preMaterialize()
      res1
    })
    .to(Sink.foreach(println))
    .run()
}
