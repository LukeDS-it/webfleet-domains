package it.ldsoftware.webfleet.domains.service.impl

import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.read.dbio.AccessListDBIO
import it.ldsoftware.webfleet.domains.read.model.AccessList
import it.ldsoftware.webfleet.domains.service.DomainReadService
import it.ldsoftware.webfleet.domains.service.model._

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class SlickDomainReadService(db: Database)(implicit ec: ExecutionContext)
  extends DomainReadService
    with LazyLogging {

  val accessList = TableQuery[AccessListDBIO]

  def insertRule(content: AccessList): Future[AccessList] =
    db.run(accessList.returning(accessList) += content)

  override def editRule(id: String, user: String, title: Option[String], icon: Option[String]): Future[Int] = {
    ???
  }

  override def deleteRule(id: String, user: String): Future[Int] =
    db.run(accessList.filter(_.id === id).filter(_.user === user).delete)

  override def search(filter: DomainFilter): Future[ServiceResult[List[AccessList]]] = {
    val query = accessList
      .filter(c => c.user === filter.user)
      .filterOpt(filter.path)((c, path) => c.id === path)
      .filterOpt(filter.title)((c, title) => c.title.toLowerCase.like(s"%${title.toLowerCase}%"))
      .result

    db.run(query)
      .map { seq =>
        logger.debug(s"""${query.statements.mkString(" ")} found ${seq.size} results""")
        success(seq.toList)
      }
  }
}

// $COVERAGE-ON$
