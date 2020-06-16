package it.ldsoftware.webfleet.domains.http.model.out

import it.ldsoftware.webfleet.domains.actors.model.WebDomain
import it.ldsoftware.webfleet.domains.security.Permissions

case class PermissionInfo(permissions: Set[String])

case object PermissionInfo {
  def apply(user: String, domain: WebDomain): PermissionInfo =
    if (user == domain.creator) PermissionInfo(Permissions.AllPermissions)
    else PermissionInfo(domain.accessList.getOrElse(user, Set()))
}
