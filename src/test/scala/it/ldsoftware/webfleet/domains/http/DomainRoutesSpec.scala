package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.auto._
import it.ldsoftware.webfleet.commons.security.User
import it.ldsoftware.webfleet.commons.service.model._
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.security.Permissions
import it.ldsoftware.webfleet.domains.service.model._
import it.ldsoftware.webfleet.domains.service.{DomainReadService, DomainService}
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class DomainRoutesSpec extends BaseHttpSpec {

  "The GET root path" should {
    "return a list of accessible domains" in {
      val uri = Uri("/api/v1/domains")

      val domainService = mock[DomainService]
      val readService = mock[DomainReadService]
      val expected = List(AccessGrant("/", "a", "b", "user"))

      when(readService.search(DomainFilter(None, None, "me")))
        .thenReturn(Future.successful(success(expected)))

      when(defaultExtractor.extractUser(CorrectJWT, None))
        .thenReturn(Future.successful(Some(User("me", Set(), Some(CorrectJWT)))))

      HttpRequest(uri = uri) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new DomainRoutes(domainService, readService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[List[AccessGrant]] shouldBe expected
        }
    }
  }

  "The POST root path" should {
    "map to the domain creation function" in {
      val uri = Uri("/api/v1/domains")

      val domainService = mock[DomainService]
      val readService = mock[DomainReadService]

      val form = CreateForm("Title", "id")
      val user = User("name", Set(), None)

      when(domainService.createDomain(form, user)).thenReturn(Future.successful(created("id")))
      when(defaultExtractor.extractUser(CorrectJWT, None))
        .thenReturn(Future.successful(Some(user)))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e => HttpRequest(uri = uri, method = HttpMethods.POST, entity = e))
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new DomainRoutes(domainService, readService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.Created
        }

      verify(domainService).createDomain(form, user)
    }
  }

  "The GET {id} path" should {
    "return information about the domain" in {
      val uri = Uri("/api/v1/domains/website-id")

      val domainService = mock[DomainService]
      val readService = mock[DomainReadService]

      val expected = WebDomain(
        "website-id",
        "Website",
        "icon",
        "name",
        Map("user1" -> Permissions.AllPermissions, "user2" -> Permissions.AllPermissions)
      )
      val user = User("name", Permissions.AllPermissions, None)

      when(domainService.getDomainInfo("website-id"))
        .thenReturn(Future.successful(success(expected)))
      when(defaultExtractor.extractUser(CorrectJWT, Some("website-id")))
        .thenReturn(Future.successful(Some(user)))

      HttpRequest(uri = uri, method = HttpMethods.GET) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new DomainRoutes(domainService, readService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.OK
          entityAs[WebDomain] shouldBe expected
        }
    }
  }

  "The PUT {id} path" should {
    "call the update domain function" in {
      val uri = Uri("/api/v1/domains/website-id")

      val domainService = mock[DomainService]
      val readService = mock[DomainReadService]

      val form = UpdateForm(None, None)
      val user = User("name", Set(), None)

      when(domainService.updateDomain("website-id", form, user))
        .thenReturn(Future.successful(noOutput))
      when(defaultExtractor.extractUser(CorrectJWT, Some("website-id")))
        .thenReturn(Future.successful(Some(user)))

      val req = Marshal(form)
        .to[RequestEntity]
        .map(e => HttpRequest(uri = uri, method = HttpMethods.PUT, entity = e))
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new DomainRoutes(domainService, readService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }

      verify(domainService).updateDomain("website-id", form, user)
    }
  }

  "The DELETE {id} path" should {
    "call the delete domain function" in {
      val uri = Uri("/api/v1/domains/website-id")

      val domainService = mock[DomainService]
      val readService = mock[DomainReadService]

      val user = User("name", Set(), None)

      when(domainService.deleteDomain("website-id", user)).thenReturn(Future.successful(noOutput))
      when(defaultExtractor.extractUser(CorrectJWT, Some("website-id")))
        .thenReturn(Future.successful(Some(user)))

      HttpRequest(uri = uri, method = HttpMethods.DELETE) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new DomainRoutes(domainService, readService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }

      verify(domainService).deleteDomain("website-id", user)
    }
  }
}
