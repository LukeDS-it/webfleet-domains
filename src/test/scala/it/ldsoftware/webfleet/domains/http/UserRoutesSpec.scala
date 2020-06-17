package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.OAuth2BearerToken
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.http.model.in.UserIn
import it.ldsoftware.webfleet.domains.http.utils.BaseHttpSpec
import it.ldsoftware.webfleet.domains.security.{Permissions, User}
import it.ldsoftware.webfleet.domains.service.DomainService
import it.ldsoftware.webfleet.domains.service.model._
import org.mockito.Mockito.{verify, when}

import scala.concurrent.Future

class UserRoutesSpec extends BaseHttpSpec {

  "The POST {id}/users path" should {
    "call the add user function" in {
      val uri = Uri("/api/v1/domains/website-id/users")

      val domainService = mock[DomainService]

      val user = User("name", Permissions.AllPermissions, None)
      when(domainService.addUser("website-id", "user", Permissions.AllPermissions))
        .thenReturn(Future.successful(noOutput))
      when(defaultExtractor.extractUser(CorrectJWT, Some("website-id")))
        .thenReturn(Future.successful(Some(user)))

      val req = Marshal(UserIn("user", Permissions.AllPermissions))
        .to[RequestEntity]
        .map(e => HttpRequest(uri = uri, method = HttpMethods.POST, entity = e))
        .futureValue

      req ~> addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new UserRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }

      verify(domainService).addUser("website-id", "user", Permissions.AllPermissions)
    }
  }

  "The DELETE {id}/users/{name} path" should {
    "call the remove user function" in {
      val uri = Uri("/api/v1/domains/website-id/users/user")

      val domainService = mock[DomainService]

      val user = User("name", Permissions.AllPermissions, None)
      when(domainService.removeUser("website-id", "user"))
        .thenReturn(Future.successful(noOutput))
      when(defaultExtractor.extractUser(CorrectJWT, Some("website-id")))
        .thenReturn(Future.successful(Some(user)))

      HttpRequest(uri = uri, method = HttpMethods.DELETE) ~>
        addCredentials(OAuth2BearerToken(CorrectJWT)) ~>
        new UserRoutes(domainService, defaultExtractor).routes ~>
        check {
          status shouldBe StatusCodes.NoContent
        }

      verify(domainService).removeUser("website-id", "user")
    }
  }

}
