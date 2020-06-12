package it.ldsoftware.webfleet.domains.database

import com.github.tminglei.slickpg._

// $COVERAGE-OFF$
trait ExtendedProfile extends ExPostgresProfile with PgDateSupport with PgDate2Support {
  ///
  override val api: API = new API {}

  trait API extends super.API with SimpleDateTimeImplicits with DateTimeImplicits
  ///
  val plainAPI: API with ByteaPlainImplicits with Date2DateTimePlainImplicits = new API
    with ByteaPlainImplicits
    with Date2DateTimePlainImplicits {}
}

object ExtendedProfile extends ExtendedProfile
// $COVERAGE-ON$
