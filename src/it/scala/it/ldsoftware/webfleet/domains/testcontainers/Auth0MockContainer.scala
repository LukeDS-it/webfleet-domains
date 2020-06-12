package it.ldsoftware.webfleet.domains.testcontainers

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.RSAPrivateKeySpec

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.dimafeng.testcontainers.FixedHostPortGenericContainer
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Base64
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{BindMode, Network}

class Auth0MockContainer(
    network: Network,
    jwkProvider: JwkProvider,
    key: String,
    enableLog: Boolean = false
) extends FixedHostPortGenericContainer(
      imageName = "mockserver/mockserver",
      exposedHostPort = 9999,
      exposedContainerPort = 1080,
      env = Map(
        "MOCKSERVER_INITIALIZATION_JSON_PATH" -> "/config/mockserver-auth0.json"
      ),
      classpathResourceMapping =
        List(("mockserver-auth0.json", "/config/mockserver-auth0.json", BindMode.READ_ONLY)),
      waitStrategy = Some(Wait.forLogMessage(".*started on port.*\n", 1))
    )
    with LazyLogging {

  configure { c =>
    c.setNetwork(network)
    if (enableLog) {
      c.withLogConsumer(new Slf4jLogConsumer(logger.underlying))
    }
    c.withNetworkAliases("auth0")
  }

  def jwtHeader(name: String, permissions: Set[String]): HttpHeader =
    Authorization(OAuth2BearerToken(generateToken(name, permissions)))

  lazy val pubK: RSAPublicKey = jwkProvider.get(key).getPublicKey.asInstanceOf[RSAPublicKey]

  lazy val priK: RSAPrivateKey = {
    val jwk = jwkProvider.get(key)
    val kf = KeyFactory.getInstance(jwk.getType)
    val n = jwk.getAdditionalAttributes.get("n").toString
    val d = jwk.getAdditionalAttributes.get("d").toString
    val modulus = new BigInteger(1, Base64.decodeBase64(n))
    val exponent = new BigInteger(1, Base64.decodeBase64(d))
    kf.generatePrivate(new RSAPrivateKeySpec(modulus, exponent)).asInstanceOf[RSAPrivateKey]
  }

  private def generateToken(name: String, perms: Set[String]): String =
    JWT
      .create()
      .withSubject(name)
      .withIssuer("mockAuth")
      .withAudience("test")
      .withClaim("scope", perms.mkString(" "))
      .sign(Algorithm.RSA256(pubK, priK))

}
