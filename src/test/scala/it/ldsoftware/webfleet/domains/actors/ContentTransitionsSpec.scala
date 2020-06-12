package it.ldsoftware.webfleet.domains.actors

import it.ldsoftware.webfleet.domains.actors.Domain._
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.security.User
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContentTransitionsSpec extends AnyWordSpec with Matchers {

  "The empty state" should {
    "return an Existing state when a Created event is processed" in {
      val form = CreateForm(
        "title",
        "/parent/child",
        "icon"
      )
      val user = User("name", Set(), None)
      val expected = WebDomain(
        "/parent/child",
        "title",
        "icon",
        Set("name")
      )

      NonExisting("/").process(Created(form, user)) shouldBe Existing(expected)
    }

    "throw an exception when any other event is processed" in {
      val user = User("name", Set(), None)

      an[IllegalStateException] should be thrownBy NonExisting("/").process(Deleted(user))
    }
  }

  "The existing state" should {

    "return a new Existing state with changed values when an Updated event is processed" in {
      val old = WebDomain(
        "domain-id",
        "title",
        "icon",
        Set()
      )

      val form = UpdateForm(
        title = Some("new title"),
        icon = Some("new icon")
      )

      val expected = WebDomain(
        "domain-id",
        "new title",
        "new icon",
        Set()
      )

      val user = User("user", Set(), None)

      Existing(old).process(Updated(form, user)) shouldBe Existing(expected)
    }

    "return a non existing state when a Deleted event is processed" in {
      val old = WebDomain(
        "domain-id",
        "title",
        "icon",
        Set()
      )

      val user = User("name", Set(), None)

      Existing(old).process(Deleted(user)) shouldBe NonExisting("domain-id")
    }

    "throw an exception when processing an unprocessable event" in {
      val form = CreateForm(
        "title",
        "/parent/child",
        "icon"
      )
      val user = User("name", Set(), None)
      val old = WebDomain(
        "title",
        "/parent/child",
        "icon",
        Set()
      )

      an[IllegalStateException] should be thrownBy Existing(old).process(Created(form, user))
    }
  }

}
