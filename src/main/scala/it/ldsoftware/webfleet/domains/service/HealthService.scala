package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.commons.service.model._

import scala.concurrent.Future

trait HealthService {
  def checkHealth: Future[ServiceResult[ApplicationHealth]]
}
