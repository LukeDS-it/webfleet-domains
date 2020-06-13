package it.ldsoftware.webfleet.domains.actors

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.{Config, ConfigFactory}
import it.ldsoftware.webfleet.domains.actors.Domain._
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.security.{Permissions, User}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

class DomainSpec
    extends ScalaTestWithActorTestKit(
      EventSourcedBehaviorTestKit.config.withFallback(DomainSpec.config)
    )
    with AnyWordSpecLike
    with BeforeAndAfterEach {

  private val rootTestKit =
    EventSourcedBehaviorTestKit[Command, Event, State](system, Domain("/"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    rootTestKit.clear()
  }

  "A non existing domain" should {

    "return not found when asked for its state" in {
      val result = rootTestKit.runCommand(Read)
      result.reply shouldBe NotFound("/")
      result.events shouldBe empty
    }

    "return done when creating a domain, and save the correct status" in {
      val form = domainForm
      val user = superUser
      val expectedRootContent = getExpectedContent(form, user)

      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      result.reply shouldBe Done
      result.event shouldBe Created(form, user)
      result.state shouldBe Existing(expectedRootContent)
    }

    "return validation errors when content is invalid" in {
      val form = domainForm.copy(id = "invalid path")
      val user = superUser

      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      val pathPattern =
        ValidationError("id", "The site id cannot contain symbols except - and _", "id.pattern")

      result.reply shouldBe Invalid(List(pathPattern))
      result.events shouldBe empty
    }

  }

  "An existing domain" should {

    "return its details when asked" in {
      val form = domainForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Read)

      result.reply shouldBe DomainInfo(getExpectedContent(form, user))
    }

    "return a validation error if trying to create an already existing domain" in {
      val form = domainForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Create(form, user, _))

      val alreadyExists =
        ValidationError("id", s"Domain id my-website already exists", "id.duplicate")

      result.reply shouldBe Domain.Invalid(List(alreadyExists))
    }

    "correctly update its contents" in {
      val form = domainForm
      val user = superUser
      val update = UpdateForm(title = Some("New title"))

      rootTestKit.runCommand[Response](Create(form, user, _))

      val result = rootTestKit.runCommand[Response](Update(update, user, _))

      result.reply shouldBe Done
      result.event shouldBe Updated(update, user)
      result.state.asInstanceOf[Existing].webDomain.title shouldBe "New title"
    }

    "delete itself correctly" in {
      val form = domainForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](Delete(user, _))
      result.reply shouldBe Done
      result.event shouldBe Deleted(user)
      result.state shouldBe NonExisting("my-website")

      rootTestKit.runCommand[Response](Read).reply shouldBe NotFound("my-website")
    }

    "add an user" in {
      val form = domainForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      val result = rootTestKit.runCommand[Response](AddUser("user-to-add", _))

      val expected = getExpectedContent(form, user)
      val nAccess = expected.accessList + "user-to-add"

      result.reply shouldBe Done
      result.event shouldBe UserAdded("user-to-add")
      result.state shouldBe Existing(expected.copy(accessList = nAccess))
    }

    "remove an user" in {
      val form = domainForm
      val user = superUser

      rootTestKit.runCommand[Response](Create(form, user, _))
      rootTestKit.runCommand[Response](AddUser("user-to-remove", _))
      val result = rootTestKit.runCommand[Response](RemoveUser("user-to-remove", _))

      result.reply shouldBe Done
      result.event shouldBe UserRemoved("user-to-remove")
      result.state shouldBe Existing(getExpectedContent(form, user))
    }
  }

  "The init function" should {
    "correctly initialize the actor" in {
      val ref = Domain.init(system)

      ref shouldBe an[ActorRef[_]]
    }
  }

  def domainForm: CreateForm = CreateForm(
    "Title",
    "my-website",
    "icon"
  )

  def superUser: User = User("name", Permissions.AllPermissions, None)

  def getExpectedContent(form: CreateForm, user: User): WebDomain =
    WebDomain(
      form.id,
      form.title,
      form.icon,
      Set(user.name)
    )

  def mockContent(commandTranslator: Domain.Command => Domain.Response): Unit = {
    val behavior = Behaviors.receiveMessage[Domain.Command] { msg =>
      val resp = commandTranslator(msg)
      msg.replyTo ! resp
      Behaviors.same
    }

    ClusterSharding(system).init(Entity(Domain.Key) { _ => behavior })
  }

}

object DomainSpec {
  val config: Config = ConfigFactory.load("application")
}
