package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.read.model.AccessList
import it.ldsoftware.webfleet.domains.service.model.{DomainFilter, ServiceResult}

import scala.concurrent.Future

trait DomainReadService {
  def insertRule(rm: AccessList): Future[AccessList]

  def editRule(id: String, user: String, title: Option[String], icon: Option[String]): Future[Int]

  def deleteRule(id: String, user: String): Future[Int]

  def search(filter: DomainFilter): Future[ServiceResult[List[AccessList]]]
}
