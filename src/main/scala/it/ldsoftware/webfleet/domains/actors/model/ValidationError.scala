package it.ldsoftware.webfleet.domains.actors.model

import it.ldsoftware.webfleet.domains.actors.serialization.CborSerializable

case class ValidationError(field: String, error: String, code: String) extends CborSerializable
