package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.http.model.in.UserIn
import it.ldsoftware.webfleet.domains.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.domains.security.Permissions
import it.ldsoftware.webfleet.domains.service.DomainService
import it.ldsoftware.webfleet.domains.service.model.NoResult

class UserRoutes(domainService: DomainService, val extractor: UserExtractor) extends RouteHelper {

  def routes: Route = pathPrefix("api" / "v1" / "domains" / Segment / "users") { domain =>
    pathEndOrSingleSlash {
      addUser(domain)
    } ~ path(Segment) { userName =>
      pathEndOrSingleSlash {
        removeUser(domain, userName)
      }
    }
  }

  private def addUser(domain: String): Route = post {
    authorize(domain, Permissions.Users.Add) { _ =>
      entity(as[UserIn]) { user =>
        svcCall[NoResult](domainService.addUser(domain, user.userName, user.permissions))
      }
    }
  }

  private def removeUser(domain: String, user: String): Route = delete {
    authorize(domain, Permissions.Users.Remove) { _ =>
      svcCall[NoResult](domainService.removeUser(domain, user))
    }
  }

}
