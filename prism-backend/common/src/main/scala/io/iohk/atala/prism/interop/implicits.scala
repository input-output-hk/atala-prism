package io.iohk.atala.prism.interop

import cats.data.NonEmptyList
import doobie.{Get, Meta, Read, Write}
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256Digest}
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.models.{DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.utils.DoobieImplicits.byteArraySeqMeta

import java.time.Instant
import scala.collection.compat.immutable.ArraySeq

object implicits {
  implicit val merkleRootMeta: Meta[MerkleRoot] =
    Meta[Array[Byte]].timap(arr => new MerkleRoot(Sha256Digest.fromBytes(arr)))(
      _.getHash.getValue
    )
  implicit val merkleRootRead: Read[MerkleRoot] =
    Read[Array[Byte]].map(arr => new MerkleRoot(Sha256Digest.fromBytes(arr)))

  implicit val didSuffixMeta: Meta[DidSuffix] =
    Meta[String].timap { DidSuffix.apply }(_.value)
  implicit val didSuffixGet: Get[DidSuffix] =
    Get[String].tmap { DidSuffix.apply }
  implicit val didSuffixRead: Read[DidSuffix] =
    Read[String].map { DidSuffix.apply }

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

  implicit val Sha256DigestWrite: Write[Sha256Digest] =
    Write[Array[Byte]].contramap(_.getValue)
  implicit val Sha256DigestRead: Read[Sha256Digest] =
    Read[Array[Byte]].map(Sha256Digest.fromBytes)
  implicit val Sha256DigestGet: Get[Sha256Digest] =
    Get[Array[Byte]].map(Sha256Digest.fromBytes)

  implicit val timestampInfoRead: Read[TimestampInfo] =
    Read[(Instant, Int, Int)].map { case (abt, absn, osn) =>
      new TimestampInfo(abt.toEpochMilli, absn, osn)
    }
  implicit val timestampInfoGet: Get[TimestampInfo] =
    Get.Advanced
      .other[(Instant, Int, Int)](
        NonEmptyList.of("TIMESTAMPTZ", "INTEGER", "INTEGER")
      )
      .tmap { case (abt, absn, osn) =>
        new TimestampInfo(abt.toEpochMilli, absn, osn)
      }

}
