package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.domains.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.domains.service.HealthService
import org.mockito.Mockito._

import scala.concurrent.Future

class HealthRoutesSpec extends BaseHttpSpec {

  "The /health route" should {
    "Return OK if everything is working correctly" in {
      val svc = mock[HealthService]
      val health = ApplicationHealth(Map("pgsql" -> "OK"))
      when(svc.checkHealth).thenReturn(Future.successful(success(health)))

      val request = HttpRequest(uri = "/health")

      request ~> new HealthRoutes(svc, defaultExtractor).routes ~> check {
        status shouldBe StatusCodes.OK
        entityAs[ApplicationHealth] shouldBe health
      }
    }

    "Return service unavailable if there are some system problems" in {
      val svc = mock[HealthService]
      val health = ApplicationHealth(Map("pgsql" -> "Error connecting to PGSQL"))
      when(svc.checkHealth).thenReturn(Future.successful(serviceUnavailable(health)))

      val request = HttpRequest(uri = "/health")

      request ~> new HealthRoutes(svc, defaultExtractor).routes ~> check {
        status shouldBe StatusCodes.ServiceUnavailable
        entityAs[ApplicationHealth] shouldBe health
      }
    }
  }

}
