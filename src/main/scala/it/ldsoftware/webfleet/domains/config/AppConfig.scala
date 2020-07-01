package it.ldsoftware.webfleet.domains.config

import java.time.Duration

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

  lazy val amqpUrl: String = config.appString("amqp.url")

  lazy val exchange: String = config.appString("amqp.exchange")

  lazy val domainDestination: String = config.appString("amqp.destinations.domains")

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
