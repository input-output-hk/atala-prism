package io.iohk.atala.prism.interop

import cats.data.NonEmptyList
import doobie.{Get, Meta, Read, Write}
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.{MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.{Ledger, TransactionId}
import io.iohk.atala.prism.utils.DoobieImplicits.byteArraySeqMeta

import java.time.Instant
import scala.collection.compat.immutable.ArraySeq

object implicits {
  implicit val merkleRootMeta: Meta[MerkleRoot] =
    Meta[Array[Byte]].timap(arr => new MerkleRoot(SHA256Digest.fromBytes(arr)))(_.getHash.getValue)
  implicit val merkleRootRead: Read[MerkleRoot] =
    Read[Array[Byte]].map(arr => new MerkleRoot(SHA256Digest.fromBytes(arr)))

  implicit val didSuffixMeta: Meta[DIDSuffix] =
    Meta[String].timap { new DIDSuffix(_) }(_.getValue)
  implicit val didSuffixGet: Get[DIDSuffix] =
    Get[String].tmap { new DIDSuffix(_) }
  implicit val didSuffixRead: Read[DIDSuffix] =
    Read[String].map { new DIDSuffix(_) }

  implicit val transactionIdGet: Get[TransactionId] =
    Get[ArraySeq[Byte]].tmap { TransactionId.from(_).get }
  implicit val transactionIdRead: Read[TransactionId] =
    Read[ArraySeq[Byte]].map { TransactionId.from(_).get }

  implicit val ledgerGet: Get[Ledger] =
    Get[String].tmap { Ledger.withNameInsensitive }
  implicit val ledgerRead: Read[Ledger] =
    Read[String].map { Ledger.withNameInsensitive }

  implicit val credentialBatchIdRead: Read[CredentialBatchId] =
    Read[String].map { CredentialBatchId.fromString }
  implicit val credentialBatchIdGet: Get[CredentialBatchId] =
    Get[String].map { CredentialBatchId.fromString }
  implicit val credentialBatchIdWrite: Write[CredentialBatchId] =
    Write[String].contramap(_.getId)

  implicit val SHA256DigestWrite: Write[SHA256Digest] =
    Write[Array[Byte]].contramap(_.getValue)
  implicit val SHA256DigestRead: Read[SHA256Digest] =
    Read[Array[Byte]].map(SHA256Digest.fromBytes)
  implicit val SHA256DigestGet: Get[SHA256Digest] =
    Get[Array[Byte]].map(SHA256Digest.fromBytes)

  implicit val timestampInfoRead: Read[TimestampInfo] =
    Read[(Instant, Int, Int)].map { case (abt, absn, osn) => new TimestampInfo(abt.toEpochMilli, absn, osn) }
  implicit val timestampInfoGet: Get[TimestampInfo] =
    Get.Advanced
      .other[(Instant, Int, Int)](
        NonEmptyList.of("TIMESTAMPTZ", "INTEGER", "INTEGER")
      )
      .tmap {
        case (abt, absn, osn) =>
          new TimestampInfo(abt.toEpochMilli, absn, osn)
      }

}
