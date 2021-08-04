package io.iohk.atala.prism.interop

import doobie.{Meta, Read}
import io.iohk.atala.prism.kotlin.credentials.TimestampInfo
import io.iohk.atala.prism.kotlin.crypto.{MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.kotlin.identity.DIDSuffix

object implicits {
  implicit val merkleRootMeta: Meta[MerkleRoot] =
    Meta[Array[Byte]].timap(arr => new MerkleRoot(SHA256Digest.fromBytes(arr)))(_.getHash.getValue)

  implicit val timestampInfoMeta: Read[TimestampInfo] =
    Read[(Long, Int, Int)].map { case (abt, absn, osn) => new TimestampInfo(abt, absn, osn) }

  implicit val didSuffixMeta: Meta[DIDSuffix] =
    Meta[String].timap{ new DIDSuffix(_) }(_.getValue)

}
