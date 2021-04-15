package net.carboninter.flows

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge}
import akka.stream.{FlowShape, Materializer}
import net.carboninter.Application.config

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object FetchEntryResults {

  lazy val elems = config.getInt("throttle.elements")
  lazy val per = FiniteDuration(config.getDuration("throttle.per").toNanos, TimeUnit.NANOSECONDS)

  def flow()(implicit materializer: Materializer): Flow[DayRequest, EntryResult, NotUsed] = {


    val fromRbd: Flow[DayRequest, EntryResult, NotUsed] = Flow[DayRequest]
      .filter(_.refetch)
      .throttle(elems, per)
      .via(rbdService.fetchRowsForDayFlow)

    val fromCache: Flow[DayRequest, EntryResult, NotUsed] = Flow[DayRequest]
      .filterNot(_.refetch)
      .flatMapConcat(dr => repo.resultsSource(dr))

    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[DayRequest](2)) //Fan-out operator
        val merge = builder.add(Merge[EntryResult](2)) //Fan-in operator

        val fromRbdShape = builder.add(fromRbd)
        val fromCacheShape = builder.add(fromCache)

        broadcast.out(0) ~> fromRbdShape ~> merge.in(0)
        broadcast.out(1) ~> fromCacheShape ~> merge.in(1)

        FlowShape(broadcast.in, merge.out)
      }
    )
  }
}
