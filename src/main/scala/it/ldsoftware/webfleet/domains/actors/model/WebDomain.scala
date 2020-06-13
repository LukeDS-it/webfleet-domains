package it.ldsoftware.webfleet.domains.actors.model

case class WebDomain(
    id: String,
    title: String,
    icon: String,
    creator: String,
    accessList: Map[String, Set[String]]
)
