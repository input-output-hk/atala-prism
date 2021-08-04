package io.iohk.atala.prism.interop

import doobie.{Get, Meta, Read, Write}
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.{MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import doobie.implicits.legacy.instant._

import java.time.Instant

object implicits {
  implicit val merkleRootMeta: Meta[MerkleRoot] =
    Meta[Array[Byte]].timap(arr => new MerkleRoot(SHA256Digest.fromBytes(arr)))(_.getHash.getValue)

  implicit val timestampInfoMeta: Read[TimestampInfo] =
    Read[(Instant, Int, Int)].map { case (abt, absn, osn) => new TimestampInfo(abt.toEpochMilli, absn, osn) }

  implicit val didSuffixMeta: Meta[DIDSuffix] =
    Meta[String].timap{ new DIDSuffix(_) }(_.getValue)

  implicit val didSuffixGet: Get[DIDSuffix] =
    Get[String].tmap{ new DIDSuffix(_) }

  implicit val credentialBatchIdMeta: Meta[CredentialBatchId] =
    Meta[String].timap{ new CredentialBatchId(_) }(_.getId)

  implicit val credentialBatchIdGet: Get[CredentialBatchId] =
    Get[String].tmap{ new CredentialBatchId(_) }

  implicit val SHA256DigestWrite: Write[SHA256Digest] =
    Write[Array[Byte]].contramap(_.getValue)

  implicit val credentialBatchIdWrite: Write[CredentialBatchId] =
    Write[String].contramap(_.getId)

}
