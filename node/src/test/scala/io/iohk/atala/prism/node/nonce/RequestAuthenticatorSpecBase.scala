package io.iohk.atala.prism.node.nonce

import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
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
      val keyPair = CryptoTestUtils.generateKeyPair()

      val signedRequest = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.privateKey
      )

      val signature =
        Base64.getUrlDecoder.decode(signedRequest.encodedSignature)
      signedRequest.signature must contain theSameElementsInOrderAs signature
      val requestNonce =
        Base64.getUrlDecoder.decode(signedRequest.encodedRequestNonce)
      signedRequest.requestNonce must contain theSameElementsInOrderAs requestNonce
      val requestWithNonce = requestNonce ++ request
      SecpPublicKey.checkECDSASignature(
        requestWithNonce,
        signature,
        keyPair.publicKey
      ) mustBe true
    }

    "return random nonces" in {
      val keyPair = CryptoTestUtils.generateKeyPair()

      val signedRequest1 = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.privateKey
      )
      val signedRequest2 = requestAuthenticator.signConnectorRequest(
        request,
        keyPair.privateKey
      )

      signedRequest1.encodedRequestNonce must not be signedRequest2.encodedRequestNonce
    }
  }
}
