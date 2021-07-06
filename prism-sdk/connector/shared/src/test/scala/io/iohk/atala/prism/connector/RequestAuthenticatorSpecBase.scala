package io.iohk.atala.prism.connector

import java.util.Base64

import io.iohk.atala.prism.crypto.{ECSignature, ECTrait}
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

abstract class RequestAuthenticatorSpecBase(ec: ECTrait) extends AnyWordSpec {
  private val requestAuthenticator = new RequestAuthenticator(ec)
  private val request = Array.ofDim[Byte](128)
  new Random().nextBytes(request)

  "signConnectorRequest" should {
    "return the encoded signature and nonce" in {
      val keyPair = ec.generateKeyPair()

      val signedRequest = requestAuthenticator.signConnectorRequest(request, keyPair.privateKey)

      val signature = Base64.getUrlDecoder.decode(signedRequest.encodedSignature)
      signedRequest.signature must contain theSameElementsInOrderAs signature
      val requestNonce = Base64.getUrlDecoder.decode(signedRequest.encodedRequestNonce)
      signedRequest.requestNonce must contain theSameElementsInOrderAs requestNonce
      val requestWithNonce = requestNonce ++ request
      ec.verify(requestWithNonce, keyPair.publicKey, ECSignature(signature)) mustBe true
    }

    "return random nonces" in {
      val keyPair = ec.generateKeyPair()

      val signedRequest1 = requestAuthenticator.signConnectorRequest(request, keyPair.privateKey)
      val signedRequest2 = requestAuthenticator.signConnectorRequest(request, keyPair.privateKey)

      signedRequest1.encodedRequestNonce must not be signedRequest2.encodedRequestNonce
    }
  }
}
