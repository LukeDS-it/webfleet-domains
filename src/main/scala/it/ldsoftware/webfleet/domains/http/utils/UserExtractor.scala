package it.ldsoftware.webfleet.domains.http.utils

import it.ldsoftware.webfleet.domains.security.User

import scala.concurrent.Future

trait UserExtractor {
  def extractUser(jwt: String, domain: Option[String]): Future[Option[User]]
}
