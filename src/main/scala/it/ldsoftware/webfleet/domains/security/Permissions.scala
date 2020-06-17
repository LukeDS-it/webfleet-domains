package it.ldsoftware.webfleet.domains.security

// $COVERAGE-OFF$ constants don't need testing
object Permissions {

  object Domains {
    val Read = "domains:read"
    val Update = "domains:update"
  }

  object Users {
    val Add = "users:add"
    val Remove = "users:remove"
  }

  object Contents {
    val Read = "content:read"
    val Create = "content:create"
    val Delete = "content:delete"
    val Publish = "content:publish"
    val Review = "content:review"
  }

  val AllPermissions: Set[String] = Set(
    Domains.Update,
    Domains.Read,
    Users.Add,
    Users.Remove,
    Contents.Create,
    Contents.Delete
  )

}
// $COVERAGE-ON$
