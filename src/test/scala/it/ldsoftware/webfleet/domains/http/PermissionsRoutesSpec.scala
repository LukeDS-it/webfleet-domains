package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpRequest, StatusCodes, Uri}
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.actors.model.WebDomain
import it.ldsoftware.webfleet.domains.http.model.out.PermissionInfo
import it.ldsoftware.webfleet.domains.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.domains.security.Permissions
import it.ldsoftware.webfleet.domains.service.DomainService
import it.ldsoftware.webfleet.domains.service.model._
import org.mockito.Mockito.when

import scala.concurrent.Future

class PermissionsRoutesSpec extends BaseHttpSpec {

  val domainInfo: WebDomain = WebDomain(
    "id",
    "title",
    "icon",
    "creator",
    Map(
      "allowed-user" -> Set("content:create")
    )
  )

  "The get permissions endpoint" should {
    "return a set of permissions for an user allowed to access the domain" in {
      val uri = Uri("/api/v1/domains/website-id/users/allowed-user/permissions")

      val domainService = mock[DomainService]

      when(domainService.getDomainInfo("website-id"))
        .thenReturn(Future.successful(success(domainInfo)))

      HttpRequest(uri = uri) ~>
        new PermissionsRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[PermissionInfo] shouldBe PermissionInfo(Set("content:create"))
        }
    }

    "return empty set for an user not allowed to access the domain" in {
      val uri = Uri("/api/v1/domains/website-id/users/another-user/permissions")
        .withQuery(Query("user" -> "an-user", "permission" -> Permissions.Contents.Insert))

      val domainService = mock[DomainService]

      when(domainService.getDomainInfo("website-id"))
        .thenReturn(Future.successful(success(domainInfo)))

      HttpRequest(uri = uri) ~>
        new PermissionsRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[PermissionInfo] shouldBe PermissionInfo(Set())
        }
    }
  }

}
