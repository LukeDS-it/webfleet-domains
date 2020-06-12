package it.ldsoftware.webfleet.domains.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.flows.ContentEventConsumer
import it.ldsoftware.webfleet.domains.read.model.AccessList
import it.ldsoftware.webfleet.domains.service.DomainReadService

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class ReadSideEventConsumer(readService: DomainReadService)(implicit ec: ExecutionContext)
    extends ContentEventConsumer
    with LazyLogging {

  override def consume(actorId: String, event: Domain.Event): Future[Done] = event match {
    case Domain.Created(form, user) =>
      val rm = AccessList(
        form.id,
        form.title,
        form.icon,
        user.name
      )
      logger.debug(s"Adding content $rm")
      readService.insertRule(rm).map(_ => Done)

    case Domain.Updated(form, user) =>
      logger.debug(s"Updating content $form")
      readService.editRule(actorId, user.name, form.title, form.icon).map(_ => Done)

    case Domain.Deleted(user) =>
      logger.debug(s"Deleting content $actorId")
      readService.deleteRule(actorId, user.name).map(_ => Done)

    case _ => Future.successful(Done)
  }
}
// $COVERAGE-ON$
