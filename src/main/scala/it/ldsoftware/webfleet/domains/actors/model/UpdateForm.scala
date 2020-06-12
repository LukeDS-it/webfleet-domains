package it.ldsoftware.webfleet.domains.actors.model

import it.ldsoftware.webfleet.domains.actors.serialization.CborSerializable

case class UpdateForm(
    title: Option[String] = None,
    icon: Option[String] = None
) extends CborSerializable
