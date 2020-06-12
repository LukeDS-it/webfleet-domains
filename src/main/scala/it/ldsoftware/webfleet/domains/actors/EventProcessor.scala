package it.ldsoftware.webfleet.domains.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.cluster.sharding.typed.{ClusterShardingSettings, ShardedDaemonProcessSettings}
import akka.stream.KillSwitches
import it.ldsoftware.webfleet.domains.flows.DomainFlow

import scala.concurrent.duration._

// $COVERAGE-OFF$ tested in driver
object EventProcessor {

  def apply(flow: DomainFlow): Behavior[Nothing] = Behaviors.setup[Nothing] { _ =>
    val killSwitch = KillSwitches.shared("eventProcessorSwitch")
    flow.run(killSwitch)
    Behaviors.receiveSignal[Nothing] {
      case (_, PostStop) =>
        killSwitch.shutdown()
        Behaviors.same
    }
  }

  def init(
      system: ActorSystem[_],
      flow: DomainFlow
  ): Unit = {
    val settings = ShardedDaemonProcessSettings(system)
      .withKeepAliveInterval(1.second)
      .withShardingSettings(ClusterShardingSettings(system))

    ShardedDaemonProcess(system).init[Nothing](
      flow.consumer.getClass.getSimpleName,
      1,
      _ => EventProcessor(flow),
      settings,
      None
    )
  }

}
// $COVERAGE-ON$
