package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.service.model.{ApplicationHealth, ServiceResult}

import scala.concurrent.Future

trait HealthService {
  def checkHealth: Future[ServiceResult[ApplicationHealth]]
}
