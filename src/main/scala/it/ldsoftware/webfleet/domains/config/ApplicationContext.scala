package it.ldsoftware.webfleet.domains.config

import java.sql.Connection

import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import it.ldsoftware.webfleet.domains.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.domains.flows.consumers.{KafkaEventConsumer, ReadSideEventConsumer}
import it.ldsoftware.webfleet.domains.flows.{ContentEventConsumer, OffsetManager}
import it.ldsoftware.webfleet.domains.http.utils.{Auth0UserExtractor, UserExtractor}
import it.ldsoftware.webfleet.domains.service.impl.{BasicHealthService, SlickDomainReadService}
import it.ldsoftware.webfleet.domains.service.{DomainReadService, HealthService}
import org.apache.kafka.clients.producer.KafkaProducer

import scala.concurrent.ExecutionContext

class ApplicationContext(appConfig: AppConfig)(implicit ec: ExecutionContext) {

  lazy val db: Database = Database.forConfig("slick.db")

  lazy val healthService: HealthService = new BasicHealthService(db)

  lazy val provider: JwkProvider = new JwkProviderBuilder(appConfig.jwtConfig.domain).build()

  lazy val extractor: UserExtractor =
    new Auth0UserExtractor(provider, appConfig.jwtConfig.issuer, appConfig.jwtConfig.audience)

  lazy val readService: DomainReadService = new SlickDomainReadService(db)

  lazy val connection: Connection = db.source.createConnection()

  lazy val offsetManager: OffsetManager = new OffsetManager(db)

  lazy val readSideEventConsumer = new ReadSideEventConsumer(readService)

  lazy val kafkaEventConsumer = new KafkaEventConsumer(
    new KafkaProducer[String, String](appConfig.kafkaProperties),
    appConfig.contentTopic
  )

  lazy val consumers: Seq[ContentEventConsumer] = Seq(readSideEventConsumer, kafkaEventConsumer)
}
