package io.iohk.atala.prism.connector

import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.util.Base64
import scala.util.Random

abstract class RequestAuthenticatorSpecBase extends AnyWordSpec {
  private val requestAuthenticator = new RequestAuthenticator
  private val request = Array.ofDim[Byte](128)
  new Random().nextBytes(request)

  "signConnectorRequest" should {
    "return the encoded signature and nonce" in {
      val keyPair = EC.generateKeyPair()

      val signedRequest = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.getPrivateKey
      )

      val signature =
        Base64.getUrlDecoder.decode(signedRequest.encodedSignature)
      signedRequest.signature must contain theSameElementsInOrderAs signature
      val requestNonce =
        Base64.getUrlDecoder.decode(signedRequest.encodedRequestNonce)
      signedRequest.requestNonce must contain theSameElementsInOrderAs requestNonce
      val requestWithNonce = requestNonce ++ request
      EC.verifyBytes(
        requestWithNonce,
        keyPair.getPublicKey,
        new ECSignature(signature)
      ) mustBe true
    }

    "return random nonces" in {
      val keyPair = EC.generateKeyPair()

      val signedRequest1 = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.getPrivateKey
      )
      val signedRequest2 = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.getPrivateKey
      )

      signedRequest1.encodedRequestNonce must not be signedRequest2.encodedRequestNonce
    }
  }
}
