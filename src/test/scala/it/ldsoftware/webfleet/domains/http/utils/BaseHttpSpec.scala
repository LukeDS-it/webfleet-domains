package it.ldsoftware.webfleet.domains.http.utils

import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

trait BaseHttpSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with MockitoSugar
    with FailFastCirceSupport {

  val CorrectJWT: String = "Correct-jwt"
  val WrongJWT: String = "Wrong-jwt"

  val defaultExtractor: UserExtractor = mock[UserExtractor]

}
