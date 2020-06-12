package it.ldsoftware.webfleet.domains.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Domains {
    val Create = "domain.create"
    val Update = "domain.update"
    val Invite = "domain.invite"
  }

  val AllPermissions: Set[String] = Set(
    Domains.Create, Domains.Update, Domains.Invite
  )

}
// $COVERAGE-ON$
