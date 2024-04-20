package io.iohk.atala.prism.node.nonce

import io.iohk.atala.prism.node.DIDUtil
import io.iohk.atala.prism.node.auth.utils.DIDUtils
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.protos.common_models
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global

class ClientHelperSpec extends AnyWordSpec {
  private val (keyPair, did) = DIDUtil.createUnpublishedDid

  "requestSigner" should {
    "produce a verifiable signature" in {
      val requestSigner = ClientHelper.requestSigner(
        new RequestAuthenticator,
        did,
        keyPair.privateKey
      )
      val request = common_models
        .ConnectionsStatusRequest()
        .withConnectionTokens("a b c".split(" ").toList)
      val header = requestSigner(request)

      // verify signed request
      val payload = header.requestNonce.mergeWith(request.toByteArray).toArray
      val didData =
        DIDUtils.validateDid(header.did).value.futureValue.toOption.value
      val publicKey = DIDUtils
        .findPublicKey(didData, header.keyId)
        .value
        .futureValue
        .toOption
        .value

      val verified = SecpPublicKey.checkECDSASignature(payload, header.signature.bytes, publicKey)
      verified must be(true)
    }
  }
}
