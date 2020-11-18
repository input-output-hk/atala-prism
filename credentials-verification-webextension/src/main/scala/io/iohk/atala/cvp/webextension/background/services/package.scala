package io.iohk.atala.cvp.webextension.background

import io.iohk.atala.cvp.webextension.common.ECKeyOperation.firstMasterKeyId
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.crypto.{EC, ECKeyPair}
import io.iohk.atala.prism.identity.DID
import scalapb.GeneratedMessage

package object services {
  private val requestAuthenticator = new RequestAuthenticator(EC)

  def metadataForRequest[Request <: GeneratedMessage](
      ecKeyPair: ECKeyPair,
      did: DID,
      request: Request
  ): Map[String, String] = {
    val signedConnectorRequest = requestAuthenticator.signConnectorRequest(request.toByteArray, ecKeyPair.privateKey)
    Map(
      "did" -> did.value,
      "didKeyId" -> firstMasterKeyId,
      "didSignature" -> signedConnectorRequest.encodedSignature,
      "requestNonce" -> signedConnectorRequest.encodedRequestNonce
    )
  }
}
