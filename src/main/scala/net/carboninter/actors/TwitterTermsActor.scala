package net.carboninter.actors

import akka.actor.Actor
import net.carboninter.util.Logging

class TwitterTermsActor extends Actor with Logging {
  import TwitterTermsActor._

  var currentTerms: List[String] = Nil

  override def receive: Receive = {
    case SetState(t) =>
      this.currentTerms = t
      sender() ! currentTerms
    case GetState(asker) =>
//      logger.debug("GetState from asker: " + asker + ", got: " + currentTerms.mkString(", "))
      sender() ! currentTerms
  }
}

object TwitterTermsActor {
  trait Envelope
  case class SetState(value: List[String]) extends Envelope
  case class GetState(asker: String) extends Envelope
}