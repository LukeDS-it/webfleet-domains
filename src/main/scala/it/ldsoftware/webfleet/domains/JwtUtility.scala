package it.ldsoftware.webfleet.domains

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}
import java.security.spec.RSAPrivateKeySpec
import java.time.{Duration, Instant}
import java.util.Date

import com.auth0.jwk.{JwkProvider, JwkProviderBuilder}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.ldsoftware.webfleet.domains.security.Permissions
import org.apache.commons.codec.binary.Base64

// $COVERAGE-OFF$ Not production code
object JwtUtility extends App {

  println(createJwt("name", "auth0", "audience", Permissions.AllPermissions))

  def createJwt(name: String, iss: String, aud: String, roles: Set[String]): String = {

    val key = "0i2mNOFBoWVO7IPdkZr1aeBSJz0yyUEu5h1jT85Hc8XOKMftXgV37R"
    val jwkProvider: JwkProvider = new JwkProviderBuilder("http://localhost:9999").build()

    val pubK: RSAPublicKey = jwkProvider.get(key).getPublicKey.asInstanceOf[RSAPublicKey]

    def priK: RSAPrivateKey = {
      val jwk = jwkProvider.get(key)
      val kf = KeyFactory.getInstance(jwk.getType)
      val n = jwk.getAdditionalAttributes.get("n").toString
      val d = jwk.getAdditionalAttributes.get("d").toString
      val modulus = new BigInteger(1, Base64.decodeBase64(n))
      val exponent = new BigInteger(1, Base64.decodeBase64(d))
      kf.generatePrivate(new RSAPrivateKeySpec(modulus, exponent)).asInstanceOf[RSAPrivateKey]
    }

    JWT
      .create()
      .withSubject(name)
      .withIssuer(iss)
      .withAudience(aud)
      .withClaim("scope", roles.mkString(" "))
      .withExpiresAt(Date.from(Instant.now().plus(Duration.ofHours(1))))
      .sign(Algorithm.RSA256(pubK, priK))
  }

}
// $COVERAGE-ON$
