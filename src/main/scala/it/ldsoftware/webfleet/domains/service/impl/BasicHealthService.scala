package it.ldsoftware.webfleet.domains.service.impl

import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.domains.service.HealthService
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class BasicHealthService(db: Database)(implicit ec: ExecutionContext) extends HealthService {

  override def checkHealth: Future[ServiceResult[ApplicationHealth]] = checkDBHealth.map {
    case (str, true)  => success(ApplicationHealth(Map("pgsql" -> str)))
    case (str, false) => serviceUnavailable(ApplicationHealth(Map("pgsql" -> str)))
  }

  private def checkDBHealth: Future[(String, Boolean)] =
    db.run(BasicHealthService.checkAction)
      .map(_ => ("ok", true))
      .recover(th => (th.getMessage, false))

}

object BasicHealthService {
  val checkAction = sql"select 1".as[Int]
}
