package io.iohk.atala.prism.node.interop

import cats.data.NonEmptyList
import doobie.{Get, Meta, Read, Write}
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.TimestampInfo
import io.iohk.atala.prism.node.models.{DidSuffix, Ledger, TransactionId}
import io.iohk.atala.prism.node.utils.DoobieImplicits.byteArraySeqMeta

import java.time.Instant
import scala.collection.compat.immutable.ArraySeq

object implicits {

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

  implicit val Sha256DigestWrite: Write[Sha256Hash] =
    Write[Array[Byte]].contramap(_.bytes.toArray)
  implicit val Sha256HashRead: Read[Sha256Hash] =
    Read[Array[Byte]].map(Sha256Hash.fromBytes)
  implicit val Sha256HashGet: Get[Sha256Hash] =
    Get[Array[Byte]].map(Sha256Hash.fromBytes)

  implicit val timestampInfoRead: Read[TimestampInfo] =
    Read[(Instant, Int, Int)].map { case (abt, absn, osn) =>
      TimestampInfo(abt.toEpochMilli, absn, osn)
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
