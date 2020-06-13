package it.ldsoftware.webfleet.domains.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Domains {
    val Update = "domains.update"
  }

  object Users {
    val Add = "users.add"
    val Remove = "users.remove"
  }

  object Contents {
    val Insert = "contents.insert"
    val Remove = "contents.remove"
  }

  val AllPermissions: Set[String] = Set(
    Domains.Update,
    Users.Add,
    Users.Remove,
    Contents.Insert,
    Contents.Remove
  )

}
// $COVERAGE-ON$
