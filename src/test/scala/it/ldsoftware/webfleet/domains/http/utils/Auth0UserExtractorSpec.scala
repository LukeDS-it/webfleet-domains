package it.ldsoftware.webfleet.domains.http.utils

import java.security.KeyPairGenerator
import java.security.interfaces.{RSAPrivateKey, RSAPublicKey}

import com.auth0.jwk.{Jwk, JwkProvider}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import it.ldsoftware.webfleet.domains.security.User
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}

class Auth0UserExtractorSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "The extractor" should {
    "correctly extract principal information" in {
      val kp = keyPair
      val issuer = "issuer"
      val audience = "audience"
      val keyId = "keyId"
      val s = "subject"
      val domain = "domain"
      val provider = mock[JwkProvider]
      val jwk = mock[Jwk]
      val permissionProvider = mock[PermissionProvider]

      when(provider.get(keyId)).thenReturn(jwk)
      when(jwk.getPublicKey).thenReturn(kp.getPublic)
      when(permissionProvider.getPermissions(domain, s)).thenReturn(Future.successful(Set("perm")))

      val token = JWT
        .create()
        .withKeyId(keyId)
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(s)
        .withClaim("scope", "openid edit:aggregate")
        .sign(
          Algorithm.RSA256(
            kp.getPublic.asInstanceOf[RSAPublicKey],
            kp.getPrivate.asInstanceOf[RSAPrivateKey]
          )
        )

      val subject = new Auth0UserExtractor(provider, issuer, audience, permissionProvider)

      subject.extractUser(token, Some(domain)).futureValue shouldBe Some(
        User(s, Set("openid", "edit:aggregate", "perm"), Some(token))
      )
    }
  }

  private def keyPair = {
    val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048)
    kpg.genKeyPair
  }
}
