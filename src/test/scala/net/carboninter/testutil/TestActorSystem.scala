package net.carboninter.testutil

import akka.actor.ActorSystem

trait TestActorSystem {

  implicit val system = ActorSystem()

  system.registerOnTermination {
    System.exit(0)
  }

}
