package it.ldsoftware.webfleet.domains.http.utils

import java.security.interfaces.RSAPublicKey

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.typesafe.scalalogging.LazyLogging
import it.ldsoftware.webfleet.domains.security.User

import scala.concurrent.{ExecutionContext, Future}

class Auth0UserExtractor(
    jwkProvider: JwkProvider,
    issuer: String,
    audience: String,
    permissionProvider: PermissionProvider
)(
    implicit val ec: ExecutionContext
) extends UserExtractor
    with LazyLogging {

  override def extractUser(jwt: String, domain: Option[String]): Future[Option[User]] =
    Future(jwt)
      .map(JWT.decode)
      .map(_.getKeyId)
      .map(jwkProvider.get)
      .map(_.getPublicKey.asInstanceOf[RSAPublicKey])
      .map(decodeAndVerify(jwt, _))
      .flatMap(decoded => createUser(domain, decoded, jwt))
      .map(Option(_))
      .recover { th =>
        logger.error("An error occurred extracting the jwt", th)
        None
      }

  private def decodeAndVerify(jwt: String, publicKey: RSAPublicKey): DecodedJWT =
    JWT
      .require(Algorithm.RSA256(publicKey, null))
      .withIssuer(issuer)
      .withAudience(audience)
      .build
      .verify(jwt)

  private def createUser(domain: Option[String], decoded: DecodedJWT, jwt: String): Future[User] = {
    domain match {
      case Some(value) =>
        permissionProvider
          .getPermissions(value, decoded.getSubject)
          .map(perms => User(decoded.getSubject, extractRoles(decoded) ++ perms, Some(jwt)))
      case None =>
        Future.successful(User(decoded.getSubject, extractRoles(decoded), Some(jwt)))
    }
  }

  private def extractRoles(decoded: DecodedJWT): Set[String] =
    decoded
      .getClaim("scope")
      .asString()
      .split(" ")
      .toSet

}
