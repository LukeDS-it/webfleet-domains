package it.ldsoftware.webfleet.domains.config

import java.sql.Connection

import akka.actor.typed.ActorSystem
import akka.actor.{ActorSystem => ClassicSystem}
import akka.util.Timeout
import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import it.ldsoftware.webfleet.commons.http.{Auth0UserExtractor, UserExtractor}
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.flows.consumers.{AMQPEventConsumer, ReadSideEventConsumer}
import it.ldsoftware.webfleet.domains.flows.{ContentEventConsumer, OffsetManager}
import it.ldsoftware.webfleet.domains.http.utils._
import it.ldsoftware.webfleet.domains.service.impl.{BasicHealthService, SlickDomainReadService}
import it.ldsoftware.webfleet.domains.service.{DomainReadService, HealthService}
import it.ldsoftware.webfleet.domains.util.RabbitMQUtils

import scala.concurrent.ExecutionContext

class ApplicationContext(appConfig: AppConfig)(
    implicit ec: ExecutionContext,
    system: ActorSystem[_],
    timeout: Timeout
) {

  implicit val classic: ClassicSystem = system.classicSystem

  lazy val db: Database = Database.forConfig("slick.db")

  lazy val healthService: HealthService = new BasicHealthService(db)

  lazy val provider: JwkProvider = new JwkProviderBuilder(appConfig.jwtConfig.domain).build()

  lazy val extractor: UserExtractor =
    new Auth0UserExtractor(
      provider,
      appConfig.jwtConfig.issuer,
      appConfig.jwtConfig.audience,
      new ActorBasedPermissionProvider(system)
    )

  lazy val readService: DomainReadService = new SlickDomainReadService(db)

  lazy val connection: Connection = db.source.createConnection()

  lazy val offsetManager: OffsetManager = new OffsetManager(db)

  lazy val readSideEventConsumer = new ReadSideEventConsumer(readService)

  lazy val amqpEventConsumer = new AMQPEventConsumer(amqp, appConfig.domainDestination)

  lazy val consumers: Seq[ContentEventConsumer] = Seq(readSideEventConsumer, amqpEventConsumer)

  lazy val amqp = new RabbitMQUtils(appConfig.amqpUrl, appConfig.exchange)
}
