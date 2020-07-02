package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import it.ldsoftware.webfleet.commons.http._
import it.ldsoftware.webfleet.domains.service._

// $COVERAGE-OFF$ specific route tests exist, this is just an aggregate
class AllRoutes(
    extractor: UserExtractor,
    domainService: DomainService,
    healthService: HealthService,
    readService: DomainReadService
) extends CORSHelper {

  def routes: Route =
    corsHandler(
      new DomainRoutes(domainService, readService, extractor).routes ~
        new UserRoutes(domainService, extractor).routes ~
        new PermissionsRoutes(domainService, extractor).routes ~
        new HealthRoutes(healthService, extractor).routes
    )

}
// $COVERAGE-ON$
