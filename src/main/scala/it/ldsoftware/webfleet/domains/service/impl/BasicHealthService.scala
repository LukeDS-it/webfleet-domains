package it.ldsoftware.webfleet.domains.service.impl

import it.ldsoftware.webfleet.domains.service.HealthService
import it.ldsoftware.webfleet.domains.service.model._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class BasicHealthService(db: Database)(implicit ec: ExecutionContext) extends HealthService {

  override def checkHealth: Future[ServiceResult[ApplicationHealth]] = checkDBHealth.map {
    case (str, true)  => success(ApplicationHealth(str))
    case (str, false) => serviceUnavailable(ApplicationHealth(str))
  }

  private def checkDBHealth: Future[(String, Boolean)] =
    db.run(BasicHealthService.checkAction)
      .map(_ => ("ok", true))
      .recover(th => (th.getMessage, false))

}

object BasicHealthService {
  val checkAction = sql"select 1".as[Int]
}
