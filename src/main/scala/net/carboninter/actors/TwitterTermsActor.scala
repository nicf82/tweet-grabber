package net.carboninter.actors

import akka.actor.Actor

class TwitterTermsActor extends Actor {
  import TwitterTermsActor._

  var currentTerms: List[String] = Nil

  override def receive: Receive = {
    case SetState(t) =>
      this.currentTerms = t
      sender() ! currentTerms
    case GetState =>
      sender() ! currentTerms
  }
}

object TwitterTermsActor {
  trait Envelope
  case class SetState(value: List[String]) extends Envelope
  case object GetState extends Envelope
}