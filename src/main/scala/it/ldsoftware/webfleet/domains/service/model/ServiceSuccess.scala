package it.ldsoftware.webfleet.domains.service.model

sealed trait ServiceSuccess[T]

case class Success[T](result: T) extends ServiceSuccess[T]
case class Created(path: String) extends ServiceSuccess[String]
case object NoOutput extends ServiceSuccess[NoResult]
