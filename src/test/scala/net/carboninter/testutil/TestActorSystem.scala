package net.carboninter.testutil

import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.stream.{Supervision, SystemMaterializer}
import net.carboninter.util.Logging

trait TestActorSystem extends Logging {

  implicit val actorSystem = ActorSystem()
  implicit val ec =  actorSystem.dispatcher
  implicit val materializer = SystemMaterializer(actorSystem).materializer

  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }
}
