package it.ldsoftware.webfleet.domains

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.config.{AppConfig, ApplicationContext}
import it.ldsoftware.webfleet.domains.database.Migrations

import scala.concurrent.ExecutionContext

// $COVERAGE-OFF$ tested with integration tests
object WebfleetDomainsApp extends App with LazyLogging {

  logger.info("Starting Webfleet Domains")

  implicit val ec: ExecutionContext = ExecutionContext.global

  val systemName = "webfleet-domains-system"

  lazy val appConfig = AppConfig(ConfigFactory.load())

  lazy val appContext = new ApplicationContext(appConfig)

  new Migrations(appContext.connection).executeMigration()

  val guardian = Guardian(appContext, appConfig.timeout, appConfig.serverPort)

  val system = ActorSystem[Nothing](guardian, systemName, appConfig.getConfig)

}
// $COVERAGE-ON$
