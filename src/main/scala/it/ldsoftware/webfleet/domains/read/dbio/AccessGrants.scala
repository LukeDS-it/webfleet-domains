package it.ldsoftware.webfleet.domains.read.dbio
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import slick.lifted.ProvenShape

// $COVERAGE-OFF$
class AccessGrants(tag: Tag) extends Table[AccessGrant](tag, "access_grants") {

  def id: Rep[String] = column[String]("id")
  def title: Rep[String] = column[String]("title")
  def icon: Rep[String] = column[String]("icon")
  def user: Rep[String] = column[String]("user_name")

  def pk = primaryKey("pk", (id, user))

  def * : ProvenShape[AccessGrant] =
    (id, title, icon, user) <> (AccessGrant.tupled, AccessGrant.unapply)
}
// $COVERAGE-ON$
