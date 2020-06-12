package it.ldsoftware.webfleet.domains.testcontainers

import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import com.typesafe.scalalogging.LazyLogging
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait

class TargetContainer(
    val jdbcUrl: String,
    val globalNet: Network,
    val appVersion: String = sys.env.getOrElse("APP_VERSION", "latest"),
    val targetPort: Int = 8080
) extends FixedHostPortGenericContainer(
      imageName = s"index.docker.io/webfleet-driver:$appVersion",
      waitStrategy = Some(Wait.forLogMessage(".*listening on http port.*\n", 1)),
      exposedHostPort = targetPort,
      exposedContainerPort = 8080,
      env = Map(
        "JDBC_DATABASE_URL" -> jdbcUrl,
        "AUTH_ISSUER" -> "mockAuth",
        "AUTH_AUDIENCE" -> "test",
        "AUTH_DOMAIN" -> "http://auth0:1080",
        "LOG_FORMAT" -> "HEROKU",
        "KAFKA_BROKERS" -> "http://kafka:9092"
      )
    )
    with LazyLogging {

  configure { c =>
    c.setNetwork(globalNet)
    c.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
  }

}
