package it.ldsoftware.webfleet.domains.flows

import akka.Done
import it.ldsoftware.webfleet.domains.actors.Domain

import scala.concurrent.Future

trait ContentEventConsumer {
  def consume(actorId: String, event: Domain.Event): Future[Done]
}
