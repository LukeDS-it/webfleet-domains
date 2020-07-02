package it.ldsoftware.webfleet.domains.actors.model

import it.ldsoftware.webfleet.commons.service.model.ValidationError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CreateFormSpec extends AnyWordSpec with Matchers {

  val id = "website-id"

  "The validation function" should {

    "return no errors when the form is correct" in {
      CreateForm(
        "title",
        id,
        "icon",
      ).validationErrors shouldBe List()
    }

    "return an error when the id has forward slashes" in {
      CreateForm(
        "title",
        "id/with/slashes",
        "icon"
      ).validationErrors shouldBe List(
        ValidationError("id", "The site id cannot contain symbols except - and _", "id.pattern")
      )
    }

    "return an error if the path is invalid" in {
      CreateForm(
        "title",
        "id with spaces",
        "icon"
      ).validationErrors shouldBe List(
        ValidationError("id", "The site id cannot contain symbols except - and _", "id.pattern")
      )
    }

  }

}
