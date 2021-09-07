package io.iohk.atala.prism.credentials

import java.util.UUID

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.protos.node_models

class CredentialBatchId private (val id: String) extends AnyVal {
  override def toString: String = id
}

object CredentialBatchId {
  private val CREDENTIAL_BATCH_ID_RE = "^[0-9a-f]{64}$".r

  def fromString(id: String): Option[CredentialBatchId] = {
    if (CREDENTIAL_BATCH_ID_RE.pattern.matcher(id).matches()) Some(new CredentialBatchId(id))
    else None
  }

  def unsafeFromString(string: String): CredentialBatchId = {
    fromString(string).getOrElse(throw new IllegalArgumentException(s"Invalid CredentialBatchId $string"))
  }

  def fromDigest(digest: Sha256Digest): CredentialBatchId = {
    new CredentialBatchId(digest.hexValue)
  }

  def fromBatchData(issuerDIDSuffix: DIDSuffix, merkleRoot: MerkleRoot): CredentialBatchId = {
    val digest = Sha256Digest
      .compute(
        node_models
          .CredentialBatchData()
          .withIssuerDid(issuerDIDSuffix.value)
          .withMerkleRoot(ByteString.copyFrom(merkleRoot.hash.value.toArray))
          .toByteArray
      )

    fromDigest(digest)
  }

  def random(): CredentialBatchId =
    new CredentialBatchId(Sha256.compute(UUID.randomUUID().toString.getBytes()).hexValue)
}
