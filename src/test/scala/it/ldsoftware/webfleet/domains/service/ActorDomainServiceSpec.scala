package it.ldsoftware.webfleet.domains.service

import java.time.Duration

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import it.ldsoftware.webfleet.domains.actors.Domain
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.security.User
import it.ldsoftware.webfleet.domains.service.impl.ActorDomainService
import it.ldsoftware.webfleet.domains.service.model._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class ActorDomainServiceSpec
    extends AnyWordSpecLike
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val timeout: Duration = Duration.ofSeconds(3)
  implicit val askTimeout: Timeout = Timeout.create(timeout)

  "The getDomainInfo function" should {
    "return information of the domain" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      val expected = defaultContent
      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.DomainInfo(expected)))

      val subject = new ActorDomainService(timeout, sharding)

      subject.getDomainInfo("/").futureValue shouldBe success(expected)
    }

    "return not found when the domain does not exist" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/branch")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.NotFound("/branch")))

      val subject = new ActorDomainService(timeout, sharding)

      subject.getDomainInfo("/branch").futureValue shouldBe notFound("/branch")
    }

    "return unexpected message when the domain returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any())).thenReturn(Future.successful(Domain.Done))

      val subject = new ActorDomainService(timeout, sharding)

      val res = subject.getDomainInfo("/").futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }

  }

  "The createDomain function" should {
    "return success when the operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "path")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any())).thenReturn(Future.successful(Domain.Done))

      val subject = new ActorDomainService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createDomain(form, user).futureValue shouldBe created(form.id)
    }

    "return invalid form when the form is not valid" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      val errs = List(ValidationError("a", "b", "c"))

      when(sharding.entityRefFor(Domain.Key, "path")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.Invalid(errs)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createDomain(form, user).futureValue shouldBe invalid(errs)
    }

    "return an unexpected failure if there was something wrong" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      val err = new Exception("Error while creating content")

      when(sharding.entityRefFor(Domain.Key, "path")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.UnexpectedError(err)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      subject.createDomain(form, user).futureValue shouldBe unexpectedError(
        err,
        "Error while creating content"
      )
    }

    "return unexpected message when the root returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "path")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.DomainInfo(null)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = defaultForm

      val user = User("name", Set(), None)

      val res = subject.createDomain(form, user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  "The editDomain function" should {
    "return success when the operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.Done))

      val subject = new ActorDomainService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.updateDomain("/", form, user).futureValue shouldBe noOutput
    }

    "return invalid form when the form is not valid" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      val errs = List(ValidationError("a", "b", "c"))

      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.Invalid(errs)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.updateDomain("/", form, user).futureValue shouldBe invalid(errs)
    }

    "return an unexpected failure if there was something wrong" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      val err = new Exception("Error while updating root")

      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.UnexpectedError(err)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      subject.updateDomain("/", form, user).futureValue shouldBe unexpectedError(
        err,
        "Error while updating root"
      )
    }

    "return unexpected message when the root returns an unexpected message" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.DomainInfo(null)))

      val subject = new ActorDomainService(timeout, sharding)

      val form = editForm

      val user = User("name", Set(), None)

      val res = subject.updateDomain("/", form, user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }

  }

  "The deleteDomain function" should {
    "return no output when operation was completed" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/child")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.Done))

      val subject = new ActorDomainService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteDomain("/child", user).futureValue shouldBe noOutput
    }

    "return not found when trying to delete a non existing branch" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/child")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.NotFound("/child")))

      val subject = new ActorDomainService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteDomain("/child", user).futureValue shouldBe notFound("/child")
    }

    "return insufficient permissions if the user doesn't have enough permissions" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/child")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.UnAuthorized))

      val subject = new ActorDomainService(timeout, sharding)

      val user = User("name", Set(), None)

      subject.deleteDomain("/child", user).futureValue shouldBe forbidden
    }

    "return unexpected message in all other cases" in {
      val sharding = mock[ClusterSharding]
      val entity = mock[EntityRef[Domain.Command]]

      when(sharding.entityRefFor(Domain.Key, "/child")).thenReturn(entity)
      when(entity.ask[Domain.Response](any())(any()))
        .thenReturn(Future.successful(Domain.DomainInfo(null)))

      val subject = new ActorDomainService(timeout, sharding)

      val user = User("name", Set(), None)

      val res = subject.deleteDomain("/child", user).futureValue
      res should be(Symbol("left"))
      res.swap.getOrElse(null) shouldBe an[UnexpectedError]
    }
  }

  def defaultContent: WebDomain = WebDomain(
    "website-id",
    "Website title",
    "i",
    "name",
    Set()
  )

  def defaultForm: CreateForm = CreateForm(
    "title",
    "path",
    "icon",
  )

  def editForm: UpdateForm = UpdateForm(
    Some("title"),
    Some("icon")
  )

}
