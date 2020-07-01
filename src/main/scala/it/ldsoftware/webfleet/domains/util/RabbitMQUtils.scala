package it.ldsoftware.webfleet.domains.util

import com.rabbitmq.client._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import it.ldsoftware.webfleet.domains.actors.Domain.Event

class RabbitMQUtils(url: String, exchange: String) extends ConnectionFactory {

  setUri(url)
  private val connection = newConnection()
  private val channel = connection.createChannel()

  channel.exchangeDeclare(exchange, "direct", true)

  def publish(destination: String, entityId: String, value: Event): Unit = {
    val envelope = RabbitEnvelope(entityId, value)
    channel.basicPublish(exchange, destination, null, envelope.asJson.noSpaces.getBytes)
  }

  def createQueueFor(destination: String): String = {
    val queueName = channel.queueDeclare().getQueue
    channel.queueBind(queueName, exchange, destination)
    queueName
  }

  def consume(queueName: String)(consumer: Option[RabbitEnvelope[Event]] => Any): Unit = {
    val callback: DeliverCallback = (_, b) => {
      decode[RabbitEnvelope[Event]](new String(b.getBody)) match {
        case Left(_)      => consumer(None)
        case Right(value) => consumer(Some(value))
      }
    }

    val cancel: CancelCallback = _ => {}

    channel.basicConsume(queueName, true, callback, cancel)
  }
}
