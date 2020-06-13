package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.actors.model.{CreateForm, UpdateForm, WebDomain}
import it.ldsoftware.webfleet.domains.security.User
import it.ldsoftware.webfleet.domains.service.model.{NoResult, ServiceResult}

import scala.concurrent.Future

trait DomainService {

  def getDomainInfo(domain: String): Future[ServiceResult[WebDomain]]

  def createDomain(form: CreateForm, user: User): Future[ServiceResult[String]]

  def updateDomain(domain: String, form: UpdateForm, user: User): Future[ServiceResult[NoResult]]

  def deleteDomain(domain: String, user: User): Future[ServiceResult[NoResult]]

  def addUser(
      domain: String,
      user: String,
      permissions: Set[String]
  ): Future[ServiceResult[NoResult]]

  def removeUser(domain: String, user: String): Future[ServiceResult[NoResult]]

  def checkPermissions(domain: String, user: String, perm: String): Future[ServiceResult[NoResult]]

}
