package it.ldsoftware.webfleet.domains.service.impl

import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.read.dbio.AccessGrants
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.service.DomainReadService
import it.ldsoftware.webfleet.domains.service.model._

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
class SlickDomainReadService(db: Database)(implicit ec: ExecutionContext)
    extends DomainReadService
    with LazyLogging {

  val accessList = TableQuery[AccessGrants]

  override def getAnyRule(domain: String): Future[AccessGrant] =
    db.run(accessList.filter(_.id === domain).result.head)

  def insertRule(content: AccessGrant): Future[AccessGrant] =
    db.run(accessList.returning(accessList) += content)

  override def editRule(dom: String, title: Option[String], icon: Option[String]): Future[Int] = {
    val action = (title, icon) match {
      case (Some(t), Some(d)) =>
        sqlu"update access_grants set title = $t, icon = $d where id = $dom"
      case (Some(t), None) =>
        sqlu"update access_grants set title = $t where id = $dom"
      case (None, Some(d)) =>
        sqlu"update access_grants set icon = $d where id = $dom"
      case (None, None) =>
        DBIO.successful(1)
    }
    db.run(action)
  }

  override def deleteRule(domain: String, user: String): Future[Int] =
    db.run(accessList.filter(_.id === domain).filter(_.user === user).delete)

  def deleteAllRules(domain: String): Future[Int] =
    db.run(accessList.filter(_.id === domain).delete)

  override def search(filter: DomainFilter): Future[ServiceResult[List[AccessGrant]]] = {
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
