package it.ldsoftware.webfleet.domains.http.utils

trait RestMapper[T, R] {
  def map(t: T): R
}
