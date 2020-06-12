package it.ldsoftware.webfleet.domains.config

import java.time.Duration
import java.util.Properties

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.config.AppConfig._

// $COVERAGE-OFF$
case class AppConfig(private val config: Config) extends LazyLogging {

  lazy val serverPort: Int = config.appInt("server.port")

  lazy val timeout: Duration = config.appDuration("timeout")

  lazy val jwtConfig: JwtConfig = JwtConfig(
    config.appString("auth0.audience"),
    config.appString("auth0.domain"),
    config.appString("auth0.issuer")
  )

  lazy val kafkaProperties: Properties = {
    val props = new Properties

    props.put("bootstrap.servers", config.appString("kafka.broker-list"))
    props.put("client.id", "webfleet-domains")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    if (config.appBoolean("kafka.sasl")) {
      val kafkaUser = config.appString("kafka.user")
      val kafkaPass = config.appString("kafka.pass")
      val jaasCfg =
        s"""org.apache.kafka.common.security.scram.ScramLoginModule required username="$kafkaUser" password="$kafkaPass";"""

      props.put("security.protocol", "SASL_SSL")
      props.put("sasl.mechanism", "SCRAM-SHA-256")
      props.put("sasl.jaas.config", jaasCfg)
    }

    props
  }

  lazy val contentTopic: String = config.appString("kafka.topics.content")

  def getConfig: Config = config

}

case object AppConfig {
  val PropsPath = "it.ldsoftware.webfleet.domains"

  implicit class ConfigOps(config: Config) {
    def appInt(path: String): Int = config.getInt(s"$PropsPath.$path")
    def appString(path: String): String = config.getString(s"$PropsPath.$path")
    def appBoolean(path: String): Boolean = config.getBoolean(s"$PropsPath.$path")
    def appDuration(path: String): Duration = config.getDuration(s"$PropsPath.$path")
  }
}

case class JwtConfig(audience: String, domain: String, issuer: String)
// $COVERAGE-ON$
