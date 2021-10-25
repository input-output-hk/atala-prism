package io.iohk.atala.prism.management.console.clients

import io.iohk.atala.prism.DIDUtil
import io.iohk.atala.prism.auth.grpc.SignedRequestsHelper
import io.iohk.atala.prism.auth.utils.DIDUtils
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.protos.connector_api
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
        keyPair.getPrivateKey
      )
      val request = connector_api
        .ConnectionsStatusRequest()
        .withConnectionTokens("a b c".split(" ").toList)
      val header = requestSigner(request)

      // verify signed request
      val payload = SignedRequestsHelper
        .merge(header.requestNonce, request.toByteArray)
        .toArray
      val didData =
        DIDUtils.validateDid(header.did).value.futureValue.toOption.value
      val publicKey = DIDUtils
        .findPublicKey(didData, header.keyId)
        .value
        .futureValue
        .toOption
        .value

      val verified = EC.verifyBytes(payload, publicKey, header.signature)
      verified must be(true)
    }
  }
}
