package it.ldsoftware.webfleet.domains.http.utils

import it.ldsoftware.webfleet.domains.security.User

trait UserExtractor {
  def extractUser(jwt: String): Option[User]
}
