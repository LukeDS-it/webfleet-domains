package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.domains.service.DomainService
import it.ldsoftware.webfleet.domains.service.model.NoResult

class PermissionsRoutes(domainService: DomainService, val extractor: UserExtractor)
    extends RouteHelper {

  def routes: Route =
    pathPrefix("api" / "v1" / "domains" / Segment / "permissions") { domain =>
      parameters("user", "permission") { (user: String, permission: String) =>
        svcCall[NoResult](domainService.checkPermissions(domain, user, permission))
      }
    }

}
