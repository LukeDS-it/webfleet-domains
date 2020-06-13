package it.ldsoftware.webfleet.domains.service

import it.ldsoftware.webfleet.domains.actors.model.{CreateForm, UpdateForm, WebDomain}
import it.ldsoftware.webfleet.domains.security.User
import it.ldsoftware.webfleet.domains.service.model.{NoResult, ServiceResult}

import scala.concurrent.Future

trait DomainService {

  def getDomainInfo(path: String): Future[ServiceResult[WebDomain]]

  def createDomain(form: CreateForm, user: User): Future[ServiceResult[String]]

  def updateDomain(path: String, form: UpdateForm, user: User): Future[ServiceResult[NoResult]]

  def deleteDomain(path: String, user: User): Future[ServiceResult[NoResult]]

  def addUser(path: String, user: String): Future[ServiceResult[NoResult]]

  def removeUser(path: String, user: String): Future[ServiceResult[NoResult]]

}
