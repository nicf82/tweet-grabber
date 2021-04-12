package net.carboninter.testutil

import akka.actor.ActorSystem
import akka.stream.{Supervision, SystemMaterializer}
import net.carboninter.Application.logger

trait TestActorSystem {

  implicit val actorSystem = ActorSystem()
  implicit val ec =  actorSystem.dispatcher
  implicit val materializer = SystemMaterializer(actorSystem).materializer

  val logAndStopDecider: Supervision.Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }
}
