package it.ldsoftware.webfleet.domains.read.dbio
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.read.model.AccessList
import slick.lifted.ProvenShape

// $COVERAGE-OFF$
class AccessListDBIO(tag: Tag) extends Table[AccessList](tag, "contents") {

  def id: Rep[String] = column[String]("id")
  def title: Rep[String] = column[String]("title")
  def icon: Rep[String] = column[String]("icon")
  def user: Rep[String] = column[String]("user")

  def pk = primaryKey("pk", (id, user))

  def * : ProvenShape[AccessList] =
    (id, title, icon, user) <> (AccessList.tupled, AccessList.unapply)
}
// $COVERAGE-ON$
