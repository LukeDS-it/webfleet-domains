package it.ldsoftware.webfleet.domains

import java.time.Duration
import java.util.{Collections, Properties}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{Uri, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, MultipleContainers}
import com.typesafe.config.ConfigFactory
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._
import it.ldsoftware.webfleet.domains.actors.model._
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.http.model.in.UserIn
import it.ldsoftware.webfleet.domains.http.model.out.PermissionInfo
import it.ldsoftware.webfleet.domains.read.model.AccessGrant
import it.ldsoftware.webfleet.domains.security.Permissions
import it.ldsoftware.webfleet.domains.service.model.ApplicationHealth
import it.ldsoftware.webfleet.domains.testcontainers._
import it.ldsoftware.webfleet.domains.utils.ResponseUtils
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.scalatest.GivenWhenThen
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.Network

import scala.concurrent.ExecutionContext

class WebfleetDomainsAppSpec
    extends AnyFeatureSpec
    with GivenWhenThen
    with Matchers
    with ForAllTestContainer
    with ScalaFutures
    with IntegrationPatience
    with FailFastCirceSupport
    with ResponseUtils
    with Eventually {

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val jwkKeyId = "0i2mNOFBoWVO7IPdkZr1aeBSJz0yyUEu5h1jT85Hc8XOKMftXgV37R"
  lazy val provider: JwkProvider = new JwkProviderBuilder("http://localhost:9999").build()

  val network: Network = Network.newNetwork()

  lazy val pgsql = new PgsqlContainer(network)

  lazy val auth0Server = new Auth0MockContainer(network, provider, jwkKeyId)

  lazy val kafka = new CustomKafkaContainer(network)

  lazy val targetContainer =
    new TargetContainer(
      jdbcUrl = s"jdbc:postgresql://pgsql:5432/webfleet",
      globalNet = network
    )

  override val container: Container = MultipleContainers(pgsql, auth0Server, kafka, targetContainer)

  implicit lazy val system: ActorSystem = ActorSystem("test-webfleet-domains")
  implicit lazy val materializer: Materializer = Materializer(system)
  lazy val http: HttpExt = Http(system)

  lazy val db: Database = Database.forConfig(
    "slick.db",
    ConfigFactory.parseString(s"""
      |slick {
      |  profile = "slick.jdbc.PostgresProfile$$"
      |  db {
      |    url = "jdbc:postgresql://localhost:${pgsql.mappedPort(5432)}/webfleet"
      |    user = "webfleet"
      |    password = "password"
      |    driver = "org.postgresql.Driver"
      |    numThreads = 5
      |    maxConnections = 5
      |    minConnections = 1
      |    connectionTimeout = 3 seconds
      |  }
      |}
      |""".stripMargin)
  )

  Feature("The service exposes a healthcheck address") {
    Scenario("The application sends an OK response when everything works fine") {
      val r = HttpRequest(uri = s"http://localhost:8080/health")
      val result = http
        .singleRequest(r)
        .flatMap(Unmarshal(_).to[ApplicationHealth])
        .futureValue

      result shouldBe ApplicationHealth("ok")
    }
  }

  Feature("The service allows to create domains") {
    Scenario("The user sends a valid creation request and is executed successfully") {
      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form = CreateForm(
        title = "Bob's website",
        id = "bob-website-123",
        icon = "flag"
      )

      val (status, headers) = createDomain(form, jwt)

      status shouldBe StatusCodes.Created
      headers should contain(Location("bob-website-123"))

      val get = HttpRequest(uri = "http://localhost:8080/api/v1/domains/bob-website-123")
        .withHeaders(Seq(jwt))
      val content = http
        .singleRequest(get)
        .flatMap(Unmarshal(_).to[WebDomain])
        .futureValue

      content.title shouldBe "Bob's website"
      content.icon shouldBe "flag"
    }

    Scenario("The user sends an invalid creation request and is rejected with an explanation") {
      val form = CreateForm(
        title = "Another website",
        id = "wickety-willow-457",
        icon = "cloud"
      )

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      createDomain(form, jwt)

      val (status, content) = Marshal(form)
        .to[RequestEntity]
        .map(e =>
          HttpRequest(
            method = HttpMethods.POST,
            uri = "http://localhost:8080/api/v1/domains",
            entity = e
          ).withHeaders(Seq(jwt))
        )
        .map(r => http.singleRequest(r))
        .flatMap(f =>
          f.flatMap(resp => Unmarshal(resp).to[List[ValidationError]].map(x => (resp.status, x)))
        )
        .futureValue

      status shouldBe StatusCodes.BadRequest
      content shouldBe List(
        ValidationError("id", "Domain id wickety-willow-457 already exists", "id.duplicate")
      )

    }
  }

  Feature("The service allows any user to see their websites") {
    Scenario("The user sees the websites they created") {
      Given("An user")
      val jwt = auth0Server.jwtHeader("an-user", Permissions.AllPermissions)

      When("The user creates a new website")

      val form = CreateForm(
        title = "ACME website",
        id = "acme-website",
        icon = "bomb"
      )

      createDomain(form, jwt)

      Then("The user sees that website in the list of sites they can access")
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(jwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("acme-website", "ACME website", "bomb", "an-user")
      }
    }
  }

  Feature("The service allows users to share their websites") {
    Scenario("The manager adds an user to their site") {
      Given("the website manager")
      val jwt = auth0Server.jwtHeader("manager", Permissions.AllPermissions)

      And("a website created by the manager")
      val form = CreateForm(
        title = "Adding users",
        id = "adding-users",
        icon = "user"
      )
      createDomain(form, jwt)
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(jwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("adding-users", "Adding users", "user", "manager")
      }

      When("the manager shares the site with another user")
      shareWith("adding-users", "shared-user", Set(Permissions.Contents.Create), jwt)
        .shouldBe(StatusCodes.NoContent)

      Then("that user can see the website in his list")
      val sharedJwt = auth0Server.jwtHeader("shared-user", Permissions.AllPermissions)
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(sharedJwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("adding-users", "Adding users", "user", "shared-user")
      }
    }

    Scenario("The manager removes an user from their site") {
      Given("the website manager")
      val jwt = auth0Server.jwtHeader("rem-manager", Permissions.AllPermissions)

      And("a website created by the manager")
      val form = CreateForm(
        title = "Removing users",
        id = "removing-users",
        icon = "user"
      )
      createDomain(form, jwt)
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(jwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("removing-users", "Removing users", "user", "rem-manager")
      }

      And("the manager had shared the site with another user")
      shareWith("removing-users", "removed-user", Set(Permissions.Contents.Create), jwt)
        .shouldBe(StatusCodes.NoContent)

      val sharedJwt = auth0Server.jwtHeader("removed-user", Permissions.AllPermissions)
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(sharedJwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("removing-users", "Removing users", "user", "removed-user")
      }

      When("the manager removes grant to that user")
      val req = HttpRequest(
        uri = "http://localhost:8080/api/v1/domains/removing-users/users/removed-user",
        method = HttpMethods.DELETE
      ).withHeaders(Seq(jwt))
      http.singleRequest(req).map(resp => resp.status).futureValue shouldBe StatusCodes.NoContent

      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(sharedJwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 0
      }
    }

    Scenario("The website creator cannot be removed from the site") {
      Given("the website manager")
      val jwt = auth0Server.jwtHeader("invincible", Permissions.AllPermissions)

      And("a website created by the manager")
      val form = CreateForm(
        title = "Manager removal",
        id = "removing-manager",
        icon = "user"
      )
      createDomain(form, jwt)
      eventually {
        val resp = http
          .singleRequest(
            HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(jwt))
          )
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("removing-manager", "Manager removal", "user", "invincible")
      }

      Then("the manager cannot be unassigned from the site")
      val req = HttpRequest(
        uri = "http://localhost:8080/api/v1/domains/removing-manager/users/invincible",
        method = HttpMethods.DELETE
      ).withHeaders(Seq(jwt))
      http.singleRequest(req).map(resp => resp.status).futureValue shouldBe StatusCodes.Forbidden
    }
  }

  Feature("The service sends data to a kafka topic") {
    Scenario("When an operation is executed, data is published on the topic") {
      val props: Properties = new Properties()
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("bootstrap.servers", s"http://localhost:${kafka.mappedPort(9093)}")
      props.put("group.id", "webfleet-test")
      props.put("enable.auto.commit", "true")

      val kafkaConsumer = new KafkaConsumer[String, String](props)
      kafkaConsumer.subscribe(Collections.singletonList("webfleet-domains"))

      val jwt = auth0Server.jwtHeader("superuser", Permissions.AllPermissions)

      val form = CreateForm(
        title = "A new website",
        id = "a-new-content",
        icon = "icon"
      )

      createDomain(form, jwt)

      eventually {
        val records = kafkaConsumer.poll(Duration.ofSeconds(1L))
        records.count() should be >= 1
      }
    }
  }

  Feature("The service allows other services to validate user permissions on a website") {
    Scenario("Users with access to a website have a set of permissions on that site") {
      Given("a website")
      val jwt = auth0Server.jwtHeader("val-01", Permissions.AllPermissions)
      val form = CreateForm("Validation OK", "validation-ok", "icon")
      createDomain(form, jwt)

      When("an user is given permission to create contents")
      shareWith("validation-ok", "val-02", Set(Permissions.Contents.Create), jwt) shouldBe StatusCodes.NoContent

      Then("the returned set of permissions should contain the insert content permission")
      val uri = Uri("http://localhost:8080/api/v1/domains/validation-ok/users/val-02/permissions")

      eventually {
        http
          .singleRequest(HttpRequest(uri = uri))
          .flatMap(res => Unmarshal(res).to[PermissionInfo])
          .futureValue shouldBe PermissionInfo(Set(Permissions.Contents.Create))
      }
    }

    Scenario("Users without access to a website have no permissions on that site") {
      Given("a website")
      val jwt = auth0Server.jwtHeader("inval-01", Permissions.AllPermissions)
      val form = CreateForm("Validation Failed", "no-access", "icon")
      createDomain(form, jwt)

      And("an user with no access on that website")

      Then("the returned set of permissions should be empty")
      val uri = Uri("http://localhost:8080/api/v1/domains/no-access/users/inval-02/permissions")

      eventually {
        http
          .singleRequest(HttpRequest(uri = uri))
          .flatMap(res => Unmarshal(res).to[PermissionInfo])
          .futureValue shouldBe PermissionInfo(Set())
      }
    }

    Scenario("The creator always has all permissions on its website") {
      Given("a website")
      val jwt = auth0Server.jwtHeader("creator", Permissions.AllPermissions)
      val form = CreateForm("Validation Failed", "creator-perms", "icon")
      createDomain(form, jwt)

      When("asking the creator's permissions")
      Then("the returned set should have all permissions")
      val uri = Uri("http://localhost:8080/api/v1/domains/creator-perms/users/creator/permissions")
      eventually {
        http
          .singleRequest(HttpRequest(uri = uri))
          .flatMap(res => Unmarshal(res).to[PermissionInfo])
          .futureValue shouldBe PermissionInfo(Permissions.AllPermissions)
      }
    }
  }

  def createDomain(form: CreateForm, jwt: HttpHeader): (StatusCode, Seq[HttpHeader]) = {
    Marshal(form)
      .to[RequestEntity]
      .map(e =>
        HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:8080/api/v1/domains",
          entity = e
        ).withHeaders(Seq(jwt))
      )
      .map(r => http.singleRequest(r))
      .flatMap(f => f.map(resp => (resp.status, resp.headers)))
      .futureValue
  }

  def shareWith(site: String, user: String, permissions: Set[String], jwt: HttpHeader): StatusCode =
    Marshal(UserIn(user, permissions))
      .to[RequestEntity]
      .map(e =>
        HttpRequest(
          method = HttpMethods.POST,
          uri = s"http://localhost:8080/api/v1/domains/$site/users",
          entity = e
        ).withHeaders(Seq(jwt))
      )
      .map(r => http.singleRequest(r))
      .flatMap(f => f.map(resp => resp.status))
      .futureValue
}
