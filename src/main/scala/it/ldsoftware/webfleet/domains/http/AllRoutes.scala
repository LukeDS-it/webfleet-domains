package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.{Directives, Route}
import it.ldsoftware.webfleet.domains.http.utils.UserExtractor
import it.ldsoftware.webfleet.domains.service.{DomainReadService, DomainService, HealthService}

// $COVERAGE-OFF$ specific route tests exist, this is just an aggregate
class AllRoutes(
    extractor: UserExtractor,
    domainService: DomainService,
    healthService: HealthService,
    readService: DomainReadService
) extends Directives {

  def routes: Route =
    new DomainRoutes(domainService, readService, extractor).routes ~
      new HealthRoutes(healthService, extractor).routes

}
// $COVERAGE-ON$
