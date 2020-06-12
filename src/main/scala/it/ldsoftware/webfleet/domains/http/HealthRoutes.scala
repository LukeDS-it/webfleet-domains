package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.domains.service.HealthService
import it.ldsoftware.webfleet.domains.service.model.ApplicationHealth

class HealthRoutes(healthService: HealthService, val extractor: UserExtractor) extends RouteHelper {
  def routes: Route = path("health") {
    get {
      svcCall[ApplicationHealth](healthService.checkHealth)
    }
  }
}
