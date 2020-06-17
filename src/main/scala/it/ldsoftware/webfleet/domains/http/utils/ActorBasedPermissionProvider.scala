package it.ldsoftware.webfleet.domains.http.utils
import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.util.Timeout
import it.ldsoftware.webfleet.domains.actors.Domain._
import it.ldsoftware.webfleet.domains.http.model.out.PermissionInfo

import scala.concurrent.{ExecutionContext, Future}

class ActorBasedPermissionProvider(system: ActorSystem[_])(
    implicit timeout: Timeout,
    ec: ExecutionContext
) extends PermissionProvider {

  private val sharding = ClusterSharding(system)

  override def getPermissions(domain: String, user: String): Future[Set[String]] =
    sharding
      .entityRefFor(Key, domain)
      .ask[Response](Read)
      .map {
        case DomainInfo(content) => PermissionInfo(user, content).permissions
        case _                   => Set()
      }
}
