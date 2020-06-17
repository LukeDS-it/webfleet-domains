package it.ldsoftware.webfleet.domains

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import akka.util.Timeout
import it.ldsoftware.webfleet.domains.actors.{Domain, EventProcessor}
import it.ldsoftware.webfleet.domains.config.{AppConfig, ApplicationContext}
import it.ldsoftware.webfleet.domains.database.Migrations
import it.ldsoftware.webfleet.domains.flows.DomainFlow
import it.ldsoftware.webfleet.domains.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.domains.service.impl._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(appConfig: AppConfig, timeout: Duration, port: Int): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext
      implicit val to: Timeout = Timeout.create(timeout)

      val sharding = ClusterSharding(system)

      lazy val appContext = new ApplicationContext(appConfig)

      val readJournal = PersistenceQuery(system.classicSystem)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      new Migrations(appContext.connection).executeMigration()

      Domain.init(system)
      appContext.consumers
        .map(new DomainFlow(readJournal, appContext.offsetManager, _))
        .foreach(EventProcessor.init(system, _))

      val domainService = new ActorDomainService(timeout, sharding)

      val routes = new AllRoutes(
        appContext.extractor,
        domainService,
        appContext.healthService,
        appContext.readService
      ).routes

      new WebfleetServer(routes, port, context.system).start()

      Behaviors.empty
    }
}
// $COVERAGE-ON$
