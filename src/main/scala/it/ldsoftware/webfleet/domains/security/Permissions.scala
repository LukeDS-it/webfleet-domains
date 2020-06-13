package it.ldsoftware.webfleet.domains.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Domains {
    val Create = "domain.create"
    val Update = "domain.update"
  }

  object Users {
    val Add = "user.add"
    val Remove = "user.remove"
  }

  val AllPermissions: Set[String] = Set(
    Domains.Create,
    Domains.Update,
    Users.Add,
    Users.Remove
  )

}
// $COVERAGE-ON$
