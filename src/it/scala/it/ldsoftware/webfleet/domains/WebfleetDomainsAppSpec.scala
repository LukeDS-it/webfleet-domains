package it.ldsoftware.webfleet.domains

import java.time.Duration
import java.util.{Collections, Properties}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
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
          .singleRequest(HttpRequest(uri = "http://localhost:8080/api/v1/domains").withHeaders(Seq(jwt)))
          .flatMap(Unmarshal(_).to[List[AccessGrant]])
          .futureValue

        resp should have size 1
        resp.head shouldBe AccessGrant("acme-website", "ACME website", "bomb", "an-user")
      }
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
}
