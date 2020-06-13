package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import it.ldsoftware.webfleet.domains.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.domains.security.Permissions
import it.ldsoftware.webfleet.domains.service.DomainService
import it.ldsoftware.webfleet.domains.service.model._
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class PermissionsRoutesSpec extends BaseHttpSpec {

  "The check permissions endpoint" should {
    "return no content for an user allowed to access the function" in {
      val uri = Uri("/api/v1/domains/website-id/permissions")
        .withQuery(Query("user" -> "an-user", "permission" -> Permissions.Contents.Insert))

      val domainService = mock[DomainService]

      when(domainService.checkPermissions("website-id", "an-user", Permissions.Contents.Insert))
        .thenReturn(Future.successful(noOutput))

      HttpRequest(uri = uri) ~>
        new PermissionsRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }

      verify(domainService).checkPermissions("website-id", "an-user", Permissions.Contents.Insert)
    }

    "return forbidden for an user not allowed to access the function" in {
      val uri = Uri("/api/v1/domains/website-id/permissions")
        .withQuery(Query("user" -> "an-user", "permission" -> Permissions.Contents.Insert))

      val domainService = mock[DomainService]

      when(domainService.checkPermissions("website-id", "an-user", Permissions.Contents.Insert))
        .thenReturn(Future.successful(forbidden))

      HttpRequest(uri = uri) ~>
        new PermissionsRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.Forbidden
        }

      verify(domainService).checkPermissions("website-id", "an-user", Permissions.Contents.Insert)
    }
  }

}
