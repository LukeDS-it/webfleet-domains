package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.service.model.{DomainFilter, ServiceResult}

import scala.concurrent.Future

trait DomainReadService {
  def getAnyRule(domain: String): Future[AccessGrant]

  def insertRule(rm: AccessGrant): Future[AccessGrant]

  def editRule(dom: String, title: Option[String], icon: Option[String]): Future[Int]

  def deleteRule(domain: String, user: String): Future[Int]

  def deleteAllRules(domain: String): Future[Int]

  def search(filter: DomainFilter): Future[ServiceResult[List[AccessGrant]]]
}
