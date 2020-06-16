package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.actors.model.WebDomain
import it.ldsoftware.webfleet.domains.http.model.out.PermissionInfo
import it.ldsoftware.webfleet.domains.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.domains.service.DomainService

class PermissionsRoutes(domainService: DomainService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route =
    pathPrefix("api" / "v1" / "domains" / Segment / "users" / Segment / "permissions") { (d, u) =>
      svcCall[WebDomain, PermissionInfo](
        domainService.getDomainInfo(d),
        di => PermissionInfo(u, di)
      )
    }

}
