package it.ldsoftware.webfleet.domains.security

case class User(name: String, permissions: Set[String] = Set(), jwt: Option[String] = None)
