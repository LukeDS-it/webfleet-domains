package it.ldsoftware.webfleet.domains.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.flows.ContentEventConsumer
import it.ldsoftware.webfleet.domains.util.RabbitMQUtils

import scala.concurrent.{ExecutionContext, Future}

class AMQPEventConsumer(amqp: RabbitMQUtils, destination: String)(implicit ec: ExecutionContext)
    extends ContentEventConsumer
    with LazyLogging {

  override def consume(actorId: String, event: Domain.Event): Future[Done] =
    Future {
      amqp.publish(destination, actorId, event)
    }.map(_ => Done)

}
