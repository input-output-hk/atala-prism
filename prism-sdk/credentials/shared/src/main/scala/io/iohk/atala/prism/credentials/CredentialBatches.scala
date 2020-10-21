package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.crypto.MerkleTree.{MerkleInclusionProof, MerkleRoot}
import io.iohk.atala.prism.crypto.{MerkleTree, SHA256Digest}

object CredentialBatches {
  def batch(signedCredentials: List[SignedCredential]): (MerkleRoot, List[MerkleInclusionProof]) = {
    MerkleTree.generateProofs(
      signedCredentials.map(cred => SHA256Digest.compute(cred.signedCredentialBytes))
    )
  }

  def verifyInclusion(
      signedCredential: SignedCredential,
      merkleRoot: MerkleRoot,
      inclusionProof: MerkleInclusionProof
  ): Boolean = {
    SHA256Digest.compute(signedCredential.signedCredentialBytes) == inclusionProof.hash &&
    MerkleTree.verifyProof(merkleRoot, inclusionProof)
  }
}
