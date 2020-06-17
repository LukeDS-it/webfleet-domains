package it.ldsoftware.webfleet.domains.http.utils

import scala.concurrent.Future

trait PermissionProvider {
  def getPermissions(domain: String, user: String): Future[Set[String]]
}
