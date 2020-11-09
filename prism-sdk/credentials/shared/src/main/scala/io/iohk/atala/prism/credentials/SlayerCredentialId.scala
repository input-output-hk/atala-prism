package io.iohk.atala.prism.credentials

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_models

/**
  * This is the id required by the slayer2 protocol, used to query details about the credential.
  *
  * @param string the underlying id as string
  */
class SlayerCredentialId private (val string: String)

object SlayerCredentialId {

  def compute(credentialHash: SHA256Digest, did: String): SlayerCredentialId = {
    val credentialData = node_models.CredentialData(
      issuer = DID.stripPrismPrefix(did),
      contentHash = ByteString.copyFrom(credentialHash.value.toArray)
    )
    val operation = node_models.AtalaOperation.Operation.IssueCredential(
      node_models.IssueCredentialOperation().withCredentialData(credentialData)
    )

    val string = SHA256Digest
      .compute(
        node_models
          .AtalaOperation(operation = operation)
          .toByteArray
      )
      .hexValue

    new SlayerCredentialId(string)
  }

  def compute(credential: SignedCredential, did: String): SlayerCredentialId =
    compute(CredentialsCryptoSDKImpl.hash(credential), did)

}
