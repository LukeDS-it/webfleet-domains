package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.domains.service.impl.BasicHealthService
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class BasicHealthServiceSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "The health check function" should {

    "return positively if all is working" in {
      val db: Database = mock[Database]
      when(db.run(BasicHealthService.checkAction)).thenReturn(Future.successful(Vector(1)))

      def subject = new BasicHealthService(db)
      subject.checkHealth.futureValue shouldBe success(ApplicationHealth(Map("pgsql" -> "ok")))
    }

    "return negatively if some services are not working" in {
      val db: Database = mock[Database]
      when(db.run(BasicHealthService.checkAction))
        .thenReturn(Future {
          throw new Exception("PGSQL Error")
        })

      def subject = new BasicHealthService(db)
      subject.checkHealth.futureValue shouldBe serviceUnavailable(ApplicationHealth(Map("pgsql" -> "PGSQL Error")))
    }

  }

}
