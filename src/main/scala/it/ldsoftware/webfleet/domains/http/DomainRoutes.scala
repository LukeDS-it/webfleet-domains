package it.ldsoftware.webfleet.domains.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.actors.model.{CreateForm, UpdateForm, WebDomain}
import it.ldsoftware.webfleet.domains.http.model.out.RestError
import it.ldsoftware.webfleet.domains.http.utils.{RouteHelper, UserExtractor}
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.service.model.{DomainFilter, NoResult}
import it.ldsoftware.webfleet.domains.service.{DomainReadService, DomainService}

class DomainRoutes(
    domainService: DomainService,
    readService: DomainReadService,
    val extractor: UserExtractor
) extends RouteHelper {

  def routes: Route = pathPrefix("api" / "v1" / "domains") {
    pathEndOrSingleSlash {
      listDomains ~ createDomain
    } ~ path(Segment) { domain =>
      pathEndOrSingleSlash {
        getDomainInfo(domain) ~ editDomain(domain) ~ deleteDomain(domain)
      }
    }
  }

  private def listDomains: Route = login { user =>
    get {
      parameterMap { params =>
        svcCall[List[AccessGrant]](
          readService.search(DomainFilter(params.get("path"), params.get("title"), user.name))
        )
      }
    }
  }

  private def createDomain: Route = login { user =>
    post {
      entity(as[CreateForm]) { form => svcCall[String](domainService.createDomain(form, user)) }
    }
  }

  private def getDomainInfo(remaining: String): Route = login { user =>
    get {
      svcCall[WebDomain](domainService.getDomainInfo(remaining))
    }
  }

  private def editDomain(remaining: String): Route = login { user =>
    put {
      entity(as[UpdateForm]) { form =>
        svcCall[NoResult](domainService.updateDomain(remaining, form, user))
      }
    }
  }

  private def deleteDomain(remaining: String): Route = login { user =>
    delete {
      if (remaining == "/") {
        complete(StatusCodes.BadRequest -> RestError("No domain specified"))
      } else {
        svcCall[NoResult](domainService.deleteDomain(remaining, user))
      }
    }
  }

}
