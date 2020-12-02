package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.MerkleTree

object CredentialBatches {
  def batch[C](
      signedCredentials: List[JsonBasedCredential[C]]
  ): (MerkleRoot, List[MerkleInclusionProof]) = {
    MerkleTree.generateProofs(
      signedCredentials.map(_.hash)
    )
  }

  def verifyInclusion[C](
      signedCredential: JsonBasedCredential[C],
      merkleRoot: MerkleRoot,
      inclusionProof: MerkleInclusionProof
  ): Boolean = {
    signedCredential.hash == inclusionProof.hash &&
    MerkleTree.verifyProof(merkleRoot, inclusionProof)
  }
}
