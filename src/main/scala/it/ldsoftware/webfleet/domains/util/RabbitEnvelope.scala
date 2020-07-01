package it.ldsoftware.webfleet.domains.util

import it.ldsoftware.webfleet.domains.actors.serialization.CborSerializable

case class RabbitEnvelope[T](entityId: String, content: T) extends CborSerializable
