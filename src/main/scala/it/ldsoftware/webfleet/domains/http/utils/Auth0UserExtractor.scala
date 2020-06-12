package it.ldsoftware.webfleet.domains.http.utils

import java.security.interfaces.RSAPublicKey

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import it.ldsoftware.webfleet.domains.security.User

class Auth0UserExtractor(jwkProvider: JwkProvider, issuer: String, audience: String)
    extends UserExtractor {

  override def extractUser(jwt: String): Option[User] =
    Some(jwt)
      .map(JWT.decode)
      .map(_.getKeyId)
      .map(jwkProvider.get)
      .map(_.getPublicKey.asInstanceOf[RSAPublicKey])
      .map(decodeJwt(jwt, _))
      .map(decoded => User(decoded.getSubject, extractRoles(decoded), Some(jwt)))

  private def decodeJwt(jwt: String, publicKey: RSAPublicKey): DecodedJWT =
    JWT
      .require(Algorithm.RSA256(publicKey, null))
      .withIssuer(issuer)
      .withAudience(audience)
      .build
      .verify(jwt)

  private def extractRoles(decoded: DecodedJWT): Set[String] =
    decoded
      .getClaim("scope")
      .asString()
      .split(" ")
      .toSet

}
