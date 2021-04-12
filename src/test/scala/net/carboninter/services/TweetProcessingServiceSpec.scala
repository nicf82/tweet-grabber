package net.carboninter.services

import akka.stream.ActorAttributes
import akka.stream.scaladsl.{Sink, Source}
import net.carboninter.Application.logAndStopDecider
import net.carboninter.models.StubTweet
import net.carboninter.repos.{EntryRepo, NewTweetRepo, TweetRepo, TweetStubRepo}
import net.carboninter.testutil.{Fixtures, TestActorSystem}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar._
import play.api.libs.json.Json
import org.scalatest.matchers.should.Matchers._
import org.mockito.Mockito.{times, verify, when}
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import reactivemongo.api.commands.{UpdateWriteResultFactory, WriteResult}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class TweetProcessingServiceSpec  extends AnyWordSpecLike with TestActorSystem with Fixtures with MockitoSugar with BeforeAndAfterEach {

  val tweetRepo     = mock[TweetRepo]
  val tweetStubRepo = mock[TweetStubRepo]
  val newTweetRepo  = mock[NewTweetRepo]
  val entryRepo     = mock[EntryRepo]

  val wr = mock[WriteResult]
  when(wr.n) thenReturn 1

  val service = new TweetProcessingService(tweetRepo, newTweetRepo, entryRepo, tweetStubRepo)

  override def beforeEach = {
    Mockito.reset(
      tweetRepo,
      tweetStubRepo,
      newTweetRepo,
      entryRepo
    )
  }

  "Running the standardIncomingTweetFlow" should {

    "Store the tweet as a stub, request entries surrounding the tweet, but no more if the entries returned did not have track/name's mentioned in the tweet" in {

      when(tweetStubRepo.store(any())) thenReturn Future.successful(wr)
      when(entryRepo.getEntriesStream(any(), any())(any())) thenReturn Source.futureSource(Future.successful(Source(List(entry1a, entry1b))))

      val done = Source.single(tweet2)
        .via(service.standardIncomingTweetFlow)
        .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
        .runWith(Sink.ignore)

      Await.result(done, Duration.Inf)

      val stubTweet = tweet2.as[StubTweet]
      verify(tweetStubRepo, times(1)).store(stubTweet)
      verify(entryRepo, times(1)).getEntriesStream(stubTweet.time, stubTweet.time.minusHours(24))
      verify(newTweetRepo, times(0)).store(any(), any())
      verify(entryRepo, times(0)).attachTweet(any(), any())
      verify(entryRepo, times(0)).attachTweet(any(), any())
    }

    "Store the tweet as a stub, request entries surrounding the tweet, and when entries returned had track/name's mentioned in the tweet, store the tweet in full and attach it to both matching entries" in {

      when(tweetStubRepo.store(any())) thenReturn Future.successful(wr)
      when(entryRepo.getEntriesStream(any(), any())(any())) thenReturn Source.futureSource(Future.successful(Source(List(entry1a, entry1b))))
      when(newTweetRepo.store(any(), any())) thenReturn Future.successful(wr)
      when(entryRepo.attachTweet(any(), any())) thenReturn Future.successful(wr)

      val done = Source.single(tweet1)
        .via(service.standardIncomingTweetFlow)
        .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
        .runWith(Sink.ignore)

      Await.result(done, Duration.Inf)

      val stubTweet = tweet1.as[StubTweet]
      verify(tweetStubRepo, times(1)).store(stubTweet)
      verify(entryRepo, times(1)).getEntriesStream(stubTweet.time, stubTweet.time.minusHours(24))
      verify(newTweetRepo, times(1)).store(stubTweet.id_str, tweet1)
      verify(entryRepo, times(1)).attachTweet(entry1a.get._id, stubTweet.id_str)
      verify(entryRepo, times(1)).attachTweet(entry1b.get._id, stubTweet.id_str)
    }

    "Store the tweet as a stub, request entries surrounding the tweet, when only entry returned was corrupt, store the tweet in full and increment entryErrors in it's stub, but don't attach it to any entries" in {

      when(tweetStubRepo.store(any())) thenReturn Future.successful(wr)
      when(entryRepo.getEntriesStream(any(), any())(any())) thenReturn Source.futureSource(Future.successful(Source(List(corruptEntry))))
      when(newTweetRepo.store(any(), any())) thenReturn Future.successful(wr)
      //      when(entryRepo.attachTweet(any(),any())) thenReturn Future.successful(wr)

      val done = Source.single(tweet1)
        .via(service.standardIncomingTweetFlow)
        .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
        .runWith(Sink.ignore)

      Await.result(done, Duration.Inf)

      val stubTweet = tweet1.as[StubTweet]
      verify(tweetStubRepo, times(1)).store(stubTweet)
      verify(entryRepo, times(1)).getEntriesStream(stubTweet.time, stubTweet.time.minusHours(24))
      verify(newTweetRepo, times(1)).store(stubTweet.id_str, tweet1)
      verify(tweetStubRepo, times(1)).incEntryErrors(stubTweet.id_str)
      verify(entryRepo, times(0)).attachTweet(entry1a.get._id, stubTweet.id_str)
      verify(entryRepo, times(0)).attachTweet(entry1b.get._id, stubTweet.id_str)
    }

    "Store the tweet as a stub, request entries surrounding the tweet, when second entry returned was corrupt, store the tweet in full and increment entryErrors in it's stub, and only attach it to the first entry" in {

      when(tweetStubRepo.store(any())) thenReturn Future.successful(wr)
      when(entryRepo.getEntriesStream(any(), any())(any())) thenReturn Source.futureSource(Future.successful(Source(List(entry1a, corruptEntry))))
      when(newTweetRepo.store(any(), any())) thenReturn Future.successful(wr)
      when(entryRepo.attachTweet(any(),any())) thenReturn Future.successful(wr)

      val done = Source.single(tweet1)
        .via(service.standardIncomingTweetFlow)
        .withAttributes(ActorAttributes.supervisionStrategy(logAndStopDecider))
        .runWith(Sink.ignore)

      Await.result(done, Duration.Inf)

      val stubTweet = tweet1.as[StubTweet]
      verify(tweetStubRepo, times(1)).store(stubTweet)
      verify(entryRepo, times(1)).getEntriesStream(stubTweet.time, stubTweet.time.minusHours(24))
      verify(newTweetRepo, times(1)).store(stubTweet.id_str, tweet1)
      verify(entryRepo, times(1)).attachTweet(entry1a.get._id, stubTweet.id_str)
      verify(entryRepo, times(0)).attachTweet(entry1b.get._id, stubTweet.id_str)
      verify(tweetStubRepo, times(1)).incEntryErrors(stubTweet.id_str)
    }
  }
}

