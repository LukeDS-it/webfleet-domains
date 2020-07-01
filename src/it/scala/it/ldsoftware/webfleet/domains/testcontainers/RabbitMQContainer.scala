package it.ldsoftware.webfleet.domains.testcontainers

import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import com.typesafe.scalalogging.LazyLogging
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait

class RabbitMQContainer(network: Network, loggingEnabled: Boolean = false)
    extends FixedHostPortGenericContainer(
      imageName = s"rabbitmq:3.8.5",
      waitStrategy = Some(Wait.forLogMessage(".*Server startup complete;.*\n", 1)),
      exposedHostPort = 5672,
      exposedContainerPort = 5672
    )
    with LazyLogging {

  configure { container =>
    container.setNetwork(network)
    container.withNetworkAliases("rabbitmq")
    if (loggingEnabled) {
      container.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
    }
  }

}
