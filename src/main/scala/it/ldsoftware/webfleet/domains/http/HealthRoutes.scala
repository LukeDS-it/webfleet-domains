package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.http.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.commons.service.model.ApplicationHealth
import it.ldsoftware.webfleet.domains.service.HealthService

class HealthRoutes(healthService: HealthService, val extractor: UserExtractor) extends RouteHelper {
  def routes: Route = path("health") {
    get {
      svcCall[ApplicationHealth](healthService.checkHealth)
    }
  }
}
