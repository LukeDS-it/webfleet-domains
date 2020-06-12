package it.ldsoftware.webfleet.domains

import java.time.Duration

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.PersistenceQuery
import it.ldsoftware.webfleet.domains.actors.{Domain, EventProcessor}
import it.ldsoftware.webfleet.domains.config.ApplicationContext
import it.ldsoftware.webfleet.domains.flows.ContentFlow
import it.ldsoftware.webfleet.domains.http.{AllRoutes, WebfleetServer}
import it.ldsoftware.webfleet.domains.service.impl._

// $COVERAGE-OFF$ Tested with integration tests
object Guardian {
  def apply(appContext: ApplicationContext, timeout: Duration, port: Int): Behavior[Nothing] =
    Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system
      import system.executionContext

      val sharding = ClusterSharding(system)

      val readJournal = PersistenceQuery(system.classicSystem)
        .readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

      Domain.init(system)
      appContext.consumers
        .map(new ContentFlow(readJournal, appContext.offsetManager, _))
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
