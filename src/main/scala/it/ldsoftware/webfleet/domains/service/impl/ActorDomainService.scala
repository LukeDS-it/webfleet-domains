package it.ldsoftware.webfleet.domains.service.impl

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import it.ldsoftware.webfleet.commons.security.User
import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.service.DomainService

import scala.concurrent.{ExecutionContext, Future}

class ActorDomainService(
    askTimeout: Duration,
    clusterSharding: ClusterSharding
)(implicit ec: ExecutionContext)
    extends DomainService {

  implicit val timeout: Timeout = Timeout.create(askTimeout)

  override def getDomainInfo(domain: String): Future[ServiceResult[WebDomain]] =
    clusterSharding
      .entityRefFor(Domain.Key, domain)
      .ask[Domain.Response](Domain.Read)
      .map {
        case Domain.DomainInfo(content) => success(content)
        case Domain.NotFound(path)      => notFound(path)
        case _                          => unexpectedMessage
      }

  override def createDomain(
      form: CreateForm,
      user: User
  ): Future[ServiceResult[String]] =
    clusterSharding
      .entityRefFor(Domain.Key, form.id)
      .ask[Domain.Response](Domain.Create(form, user, _))
      .map {
        case Domain.Done                   => created(form.id)
        case Domain.Invalid(errors)        => invalid(errors)
        case Domain.NotFound(path)         => notFound(path)
        case Domain.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case _                             => unexpectedMessage
      }

  override def updateDomain(
      domain: String,
      form: UpdateForm,
      user: User
  ): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Domain.Key, domain)
      .ask[Domain.Response](Domain.Update(form, user, _))
      .map {
        case Domain.Done                   => noOutput
        case Domain.Invalid(errors)        => invalid(errors)
        case Domain.NotFound(path)         => notFound(path)
        case Domain.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case _                             => unexpectedMessage
      }

  override def deleteDomain(domain: String, user: User): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Domain.Key, domain)
      .ask[Domain.Response](Domain.Delete(user, _))
      .map {
        case Domain.Done                   => noOutput
        case Domain.Invalid(errors)        => invalid(errors)
        case Domain.NotFound(path)         => notFound(path)
        case Domain.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case Domain.UnAuthorized           => forbidden
        case _                             => unexpectedMessage
      }

  override def addUser(
      domain: String,
      user: String,
      permissions: Set[String]
  ): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Domain.Key, domain)
      .ask[Domain.Response](Domain.AddUser(user, permissions, _))
      .map {
        case Domain.Done                   => noOutput
        case Domain.NotFound(path)         => notFound(path)
        case Domain.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case _                             => unexpectedMessage
      }

  override def removeUser(domain: String, user: String): Future[ServiceResult[NoResult]] =
    clusterSharding
      .entityRefFor(Domain.Key, domain)
      .ask[Domain.Response](Domain.RemoveUser(user, _))
      .map {
        case Domain.Done                   => noOutput
        case Domain.NotFound(path)         => notFound(path)
        case Domain.UnexpectedError(error) => unexpectedError(error, error.getMessage)
        case Domain.UnAuthorized           => forbidden
        case _                             => unexpectedMessage
      }

  private def unexpectedMessage[T]: ServiceResult[T] =
    unexpectedError(new Error(), "Unexpected response from actor")
}
