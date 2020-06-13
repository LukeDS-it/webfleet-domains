package it.ldsoftware.webfleet.domains.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.actors.serialization.CborSerializable
import it.ldsoftware.webfleet.domains.flows.DomainFlow
import it.ldsoftware.webfleet.domains.security.User

/**
  * This object contains actor logic for any content of the website.
  */
object Domain {

  type Requester = ActorRef[Response]

  val Key: EntityTypeKey[Command] = EntityTypeKey[Command]("WebDomain")

  sealed trait Command extends CborSerializable {
    def replyTo: Requester
  }

  case class Read(replyTo: Requester) extends Command
  case class Create(form: CreateForm, user: User, replyTo: Requester) extends Command
  case class Update(form: UpdateForm, user: User, replyTo: Requester) extends Command
  case class Delete(user: User, replyTo: Requester) extends Command
  case class AddUser(name: String, replyTo: Requester) extends Command
  case class RemoveUser(name: String, replyTo: Requester) extends Command

  sealed trait Event extends CborSerializable

  case class Created(form: CreateForm, user: User) extends Event
  case class Updated(form: UpdateForm, user: User) extends Event
  case class Deleted(user: User) extends Event
  case class UserAdded(name: String) extends Event
  case class UserRemoved(name: String) extends Event

  sealed trait Response extends CborSerializable

  case class DomainInfo(content: WebDomain) extends Response
  case class Invalid(errors: List[ValidationError]) extends Response
  case class NotFound(path: String) extends Response
  case class UnexpectedError(error: Throwable) extends Response
  case object Done extends Response
  case object UnAuthorized extends Response
  private def Duplicate(str: String) =
    Invalid(List(ValidationError("id", s"Domain id $str already exists", "id.duplicate")))

  /**
    * The state will know how to handle commands and process events, so that logic will be easier
    * to test and is logically organized
    */
  sealed trait State extends CborSerializable {
    def handle(command: Command): ReplyEffect[Event, State]
    def process(event: Event): State
  }

  /**
    * This state represents a non existing content. The only things it can do are
    * accepting and validating a creation request, or responding "not found" to
    * all other requests.
    *
    * It can only respond to a Created event to create a new Existing state
    *
    * @param id the id of the domain
    */
  case class NonExisting(id: String) extends State {
    override def handle(command: Command): ReplyEffect[Event, State] =
      command match {
        case Create(form, user, replyTo) =>
          form.validationErrors match {
            case Nil =>
              Effect.persist(Created(form, user)).thenReply(replyTo)(_ => Done)
            case err =>
              Effect.reply(replyTo)(Invalid(err))
          }
        case _ => Effect.reply(command.replyTo)(NotFound(id))
      }

    override def process(event: Event): State = event match {
      case Created(form, user) => Existing(form, user)
      case _                   => throw new IllegalStateException(s"Cannot process $event")
    }
  }

  /**
    * This state represents an existing content. It will reply its current state when requested;
    * accept update, delete, add child, remove child, update child requests; it will refuse
    * other creation requests.
    *
    * @param webDomain the actual content of an existing Domain
    */
  case class Existing(webDomain: WebDomain) extends State {

    override def handle(command: Command): ReplyEffect[Event, State] =
      command match {
        case Read(replyTo) =>
          Effect.reply(replyTo)(DomainInfo(webDomain))
        case Create(_, _, replyTo) =>
          Effect.reply(replyTo)(Duplicate(webDomain.id))
        case Update(form, user, replyTo) =>
          Effect.persist(Updated(form, user)).thenReply(replyTo)(_ => Done)
        case Delete(user, replyTo) =>
          Effect.persist(Deleted(user)).thenReply(replyTo)(_ => Done)
        case AddUser(user, replyTo) =>
          Effect.persist(UserAdded(user)).thenReply(replyTo)(_ => Done)
        case RemoveUser(user, replyTo) =>
          Effect.persist(UserRemoved(user)).thenReply(replyTo)(_ => Done)
      }

    override def process(event: Event): State = event match {
      case Updated(form, _) =>
        Existing(
          webDomain.copy(
            title = form.title.getOrElse(webDomain.title),
            icon = form.icon.getOrElse(webDomain.icon)
          )
        )
      case Deleted(_) =>
        NonExisting(webDomain.id)
      case UserAdded(user) =>
        Existing(webDomain.copy(accessList = webDomain.accessList + user))
      case UserRemoved(user) =>
        Existing(webDomain.copy(accessList = webDomain.accessList - user))
      case e =>
        throw new IllegalStateException(s"Cannot process $e")
    }
  }

  case object Existing {
    def apply(form: CreateForm, user: User): Existing = {
      val content = WebDomain(form.id, form.title, form.icon, user.name, Set(user.name))
      Existing(content)
    }

  }

  /**
    * This creates the behavior for the Content actor
    *
    * @param id the path of this actor, which corresponds to the http relative path to the content
    * @return the behavior of the Content actor
    */
  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId.ofUniqueId(id),
        emptyState = NonExisting(id),
        commandHandler = (state, command) => state.handle(command),
        eventHandler = (state, event) => state.process(event)
      )
      .withTagger(_ => Set(DomainFlow.Tag))

  /**
    * This function initializes the Content actor in the cluster sharding
    *
    * @param system the main actor system
    * @return the reference to the Content actor
    */
  def init(system: ActorSystem[_]): ActorRef[_] =
    ClusterSharding(system).init(Entity(Key) { context => Domain(context.entityId) })
}
