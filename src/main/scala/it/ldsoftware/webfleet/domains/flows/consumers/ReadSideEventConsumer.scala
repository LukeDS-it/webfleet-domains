package it.ldsoftware.webfleet.domains.flows.consumers

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.flows.ContentEventConsumer
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.service.DomainReadService

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class ReadSideEventConsumer(readService: DomainReadService)(implicit ec: ExecutionContext)
    extends ContentEventConsumer
    with LazyLogging {

  override def consume(actorId: String, event: Domain.Event): Future[Done] = event match {
    case Domain.Created(form, user) =>
      val rm = AccessGrant(
        form.id,
        form.title,
        form.icon,
        user.name
      )
      logger.debug(s"Adding main access grant $rm")
      readService.insertRule(rm).map(_ => Done)

    case Domain.Updated(form, _) =>
      logger.debug(s"Updating access grants for $actorId")
      readService.editRule(actorId, form.title, form.icon).map(_ => Done)

    case Domain.Deleted(_) =>
      logger.debug(s"Deleting access grants of domain $actorId")
      readService.deleteAllRules(actorId).map(_ => Done)

    case Domain.UserAdded(userName, _) =>
      logger.debug(s"Adding user $userName to domain $actorId")
      readService
        .getAnyRule(actorId)
        .map(grant => grant.copy(user = userName))
        .flatMap(readService.insertRule)
        .map(_ => Done)

    case Domain.UserRemoved(userName) =>
      logger.debug(s"Removing access from $actorId to $userName")
      readService.deleteRule(actorId, userName).map(_ => Done)

    case _ => Future.successful(Done)
  }
}
// $COVERAGE-ON$
