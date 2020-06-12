package it.ldsoftware.webfleet.domains.flows

import akka.NotUsed
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.Offset
import akka.stream.scaladsl.{RestartSource, Sink, Source}
import akka.stream.{Materializer, SharedKillSwitch}
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.flows.ContentFlow._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class ContentFlow(
    readJournal: JdbcReadJournal,
    offsetManager: OffsetManager,
    val consumer: ContentEventConsumer
)(
    implicit ec: ExecutionContext,
    mat: Materializer
) {

  val consumerName: String = consumer.getClass.getSimpleName

  def run(killSwitch: SharedKillSwitch): Unit =
    RestartSource
      .withBackoff(500.millis, maxBackoff = 20.seconds, randomFactor = 0.1) { () =>
        Source.futureSource {
          offsetManager.getLastOffset(consumerName).map { offset =>
            processEvents(offset)
              .mapAsync(1)(offsetManager.writeOffset(consumerName, _))
          }
        }
      }
      .via(killSwitch.flow)
      .runWith(Sink.ignore)

  def processEvents(offset: Long): Source[Offset, NotUsed] =
    readJournal.eventsByTag(Tag, offset).mapAsync(1) { envelope =>
      envelope.event match {
        case e: Domain.Event =>
          consumer
            .consume(envelope.persistenceId, e)
            .recover(th => println(th))
            .map(_ => envelope.offset)
        case unknown => Future.failed(new IllegalArgumentException(s"Cannot process $unknown"))
      }
    }

}

object ContentFlow {
  val Tag = "content"
}
