package io.iohk.atala.prism.interop

import doobie.Meta
import io.iohk.atala.prism.kotlin.crypto.{MerkleRoot, SHA256Digest}

object implicits {
  implicit val merkleRootMeta: Meta[MerkleRoot] =
    Meta[Array[Byte]].timap(arr => new MerkleRoot(SHA256Digest.fromBytes(arr)))(_.getHash.getValue)
}
