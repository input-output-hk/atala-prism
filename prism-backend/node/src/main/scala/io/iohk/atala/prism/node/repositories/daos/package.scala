package io.iohk.atala.prism.node.repositories

import cats.data.NonEmptyList
import doobie._
import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.{MerkleRoot, Sha256Digest}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.models._
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.protos.models.TimestampInfo
import io.iohk.atala.prism.utils.syntax._

import java.time.Instant

package object daos extends BaseDAO {

  implicit val pgKeyUsageMeta: Meta[KeyUsage] = pgEnumString[KeyUsage](
    "KEY_USAGE",
    a => KeyUsage.withNameOption(a).getOrElse(throw InvalidEnum[KeyUsage](a)),
    _.entryName
  )

  implicit val pgAtalaObjectTransactionStatusMeta: Meta[AtalaObjectTransactionSubmissionStatus] =
    pgEnumString[AtalaObjectTransactionSubmissionStatus](
      "ATALA_OBJECT_TRANSACTION_STATUS",
      a =>
        AtalaObjectTransactionSubmissionStatus
          .withNameOption(a)
          .getOrElse(
            throw InvalidEnum[AtalaObjectTransactionSubmissionStatus](a)
          ),
      _.entryName
    )

  implicit val pgAtalaObjectStatus: Meta[AtalaObjectStatus] =
    pgEnumString[AtalaObjectStatus](
      "ATALA_OBJECT_STATUS",
      a =>
        AtalaObjectStatus
          .withNameOption(a)
          .getOrElse(throw InvalidEnum[AtalaObjectStatus](a)),
      _.entryName
    )

  implicit val pgOperationStatusMeta: Meta[AtalaOperationStatus] =
    pgEnumString[AtalaOperationStatus](
      "ATALA_OPERATION_STATUS",
      a =>
        AtalaOperationStatus
          .withNameOption(a)
          .getOrElse(throw InvalidEnum[AtalaOperationStatus](a)),
      _.entryName
    )

  implicit val didSuffixPut: Put[DidSuffix] = Put[String].contramap(_.getValue)
  implicit val didSuffixGet: Get[DidSuffix] = Get[String].map(DidSuffix.apply)

  implicit val credentialIdPut: Put[CredentialId] = Put[String].contramap(_.id)
  implicit val credentialIdGet: Get[CredentialId] =
    Get[String].map(CredentialId(_))

  implicit val credentialBatchIdMeta: Meta[CredentialBatchId] =
    Meta[String].timap(CredentialBatchId.fromString)(_.getId)

  implicit val didPublicKeyWrite: Write[DIDPublicKeyState] = {
    Write[
      (
          DidSuffix,
          String,
          KeyUsage,
          String,
          Array[Byte],
          Instant,
          Int,
          Int,
          Option[Instant],
          Option[Int],
          Option[Int]
      )
    ].contramap { key =>
      val curveName = ECConfig.getCURVE_NAME
      val compressed = key.key.getEncodedCompressed
      (
        key.didSuffix,
        key.keyId,
        key.keyUsage,
        curveName,
        compressed,
        key.addedOn.timestampInfo.getAtalaBlockTimestamp.toInstant,
        key.addedOn.timestampInfo.getAtalaBlockSequenceNumber,
        key.addedOn.timestampInfo.getOperationSequenceNumber,
        key.revokedOn map (_.timestampInfo.getAtalaBlockTimestamp.toInstant),
        key.revokedOn map (_.timestampInfo.getAtalaBlockSequenceNumber),
        key.revokedOn map (_.timestampInfo.getOperationSequenceNumber)
      )
    }
  }

  implicit val didPublicKeyRead: Read[DIDPublicKeyState] = {
    Read[
      (
          DidSuffix,
          String,
          KeyUsage,
          String,
          Array[Byte],
          Instant,
          Int,
          Int,
          TransactionId,
          Ledger,
          Option[Instant],
          Option[Int],
          Option[Int],
          Option[TransactionId],
          Option[Ledger]
      )
    ].map {
      case (
            didSuffix,
            keyId,
            keyUsage,
            curveId,
            compressed,
            aTimestamp,
            aABSN,
            aOSN,
            aTransactionId,
            aLedger,
            rTimestamp,
            rABSN,
            rOSN,
            rTransactionId,
            rLedger
          ) =>
        assert(curveId == ECConfig.getCURVE_NAME)
        val javaPublicKey: ECPublicKey =
          EC.toPublicKeyFromCompressed(compressed)
        val revokeLedgerData =
          for (
            transactionId <- rTransactionId; ledger <- rLedger; t <- rTimestamp;
            absn <- rABSN; osn <- rOSN
          )
            yield LedgerData(
              transactionId = transactionId,
              ledger = ledger,
              timestampInfo = new TimestampInfo(t.toEpochMilli, absn, osn)
            )
        DIDPublicKeyState(
          didSuffix,
          keyId,
          keyUsage,
          javaPublicKey,
          LedgerData(
            transactionId = aTransactionId,
            ledger = aLedger,
            timestampInfo = new TimestampInfo(aTimestamp.toEpochMilli, aABSN, aOSN)
          ),
          revokeLedgerData
        )
    }
  }

  // added_on, added_on_absn, added_on_osn, added_on_transaction_id, ledger

  implicit val atalaObjectIdMeta: Meta[AtalaObjectId] =
    Meta[Array[Byte]].timap(value => AtalaObjectId(value.toVector))(
      _.value.toArray
    )

  implicit val atalaOperationInfoRead: Read[AtalaOperationInfo] = {
    Read[
      (
          AtalaOperationId,
          AtalaObjectId,
          AtalaOperationStatus,
          String,
          Option[AtalaObjectTransactionSubmissionStatus],
          Option[TransactionId]
      )
    ].map(
      (x: (
          AtalaOperationId,
          AtalaObjectId,
          AtalaOperationStatus,
          String,
          Option[AtalaObjectTransactionSubmissionStatus],
          Option[TransactionId]
      )) => AtalaOperationInfo(x._1, x._2, x._3, x._4, x._5, x._6)
    )
  }

  implicit val atalaObjectRead: Read[AtalaObjectInfo] = {
    Read[
      (
          AtalaObjectId,
          Array[Byte],
          AtalaObjectStatus,
          Option[TransactionId],
          Option[Ledger],
          Option[Int],
          Option[Instant],
          Option[Int]
      )
    ].map {
      case (
            objectId,
            byteContent,
            status,
            maybeTransactionId,
            maybeLedger,
            maybeBlockNumber,
            maybeBlockTimestamp,
            maybeBlockIndex
          ) =>
        AtalaObjectInfo(
          objectId,
          byteContent,
          status,
          (
            maybeTransactionId,
            maybeLedger,
            maybeBlockNumber,
            maybeBlockTimestamp,
            maybeBlockIndex
          ) match {
            case (
                  Some(transactionId),
                  Some(ledger),
                  Some(blockNumber),
                  Some(blockTimestamp),
                  Some(blockIndex)
                ) =>
              Some(
                TransactionInfo(
                  transactionId,
                  ledger,
                  Some(BlockInfo(blockNumber, blockTimestamp, blockIndex))
                )
              )
            case _ => None
          }
        )
    }
  }

  implicit val ledgerDataGet: Get[LedgerData] =
    Get.Advanced
      .other[(Array[Byte], String, Instant, Int, Int)](
        NonEmptyList.of(
          "TRANSACTION_ID",
          "VARCHAR(32)",
          "TIMESTAMPTZ",
          "INTEGER",
          "INTEGER"
        )
      )
      .tmap { case (tId, ledger, abt, absn, osn) =>
        LedgerData(
          TransactionId.from(tId).get,
          Ledger.withNameInsensitive(ledger),
          new TimestampInfo(abt.toEpochMilli, absn, osn)
        )
      }

  implicit val ledgerDataRead: Read[LedgerData] =
    Read[(Array[Byte], String, Instant, Int, Int)]
      .map { case (tId, ledger, abt, absn, osn) =>
        LedgerData(
          TransactionId.from(tId).get,
          Ledger.withNameInsensitive(ledger),
          new TimestampInfo(abt.toEpochMilli, absn, osn)
        )
      }

  implicit val CredentialBatchStateRead: Read[CredentialBatchState] = {
    Read[
      (
          String,
          String,
          Array[Byte],
          Array[Byte],
          String,
          Instant,
          Int,
          Int,
          Option[Array[Byte]],
          Option[String],
          Option[Instant],
          Option[Int],
          Option[Int],
          Array[Byte]
      )
    ].map {
      case (
            batchId,
            suffix,
            root,
            issTxId,
            issLedger,
            issABT,
            issABSN,
            issOSN,
            revTxIdOp,
            revLedgerOp,
            revABTOp,
            revABSNOp,
            revOSNOp,
            sha
          ) =>
        val issuedOn = LedgerData(
          TransactionId.from(issTxId).get,
          Ledger.withNameInsensitive(issLedger),
          new TimestampInfo(issABT.toEpochMilli, issABSN, issOSN)
        )
        val revokedOn = {
          (revTxIdOp, revLedgerOp, revABTOp, revABSNOp, revOSNOp) match {
            case (
                  Some(rTrId),
                  Some(rLedger),
                  Some(rAbt),
                  Some(rAbsn),
                  Some(rOsn)
                ) =>
              Some(
                LedgerData(
                  TransactionId.from(rTrId).get,
                  Ledger.withNameInsensitive(rLedger),
                  new TimestampInfo(rAbt.toEpochMilli, rAbsn, rOsn)
                )
              )
            case _ => None
          }
        }
        CredentialBatchState(
          CredentialBatchId.fromString(batchId),
          DidSuffix(suffix),
          new MerkleRoot(Sha256Digest.fromBytes(root)),
          issuedOn,
          revokedOn,
          Sha256Digest.fromBytes(sha)
        )
    }
  }

  implicit val protocolVersionRead: Read[ProtocolVersion] =
    Read[(Int, Int)]
      .map { case (major, minor) =>
        ProtocolVersion(major, minor)
      }

  implicit val protocolVersionInfoRead: Read[ProtocolVersionInfo] =
    Read[(Int, Int, Option[String], Int)]
      .map { case (major, minor, versionName, effectiveSince) =>
        ProtocolVersionInfo(
          ProtocolVersion(major, minor),
          versionName,
          effectiveSince
        )
      }

  implicit val transactionInfoRead: Read[TransactionInfo] =
    Read[(Array[Byte], String, Option[Int], Option[Instant], Option[Int])]
      .map { case (txId, ledger, maybeBlockNumber, maybeBlockTimestamp, maybeBlockIndex) =>
        TransactionInfo(
          TransactionId.from(txId).get,
          Ledger.withNameInsensitive(ledger),
          for {
            bn <- maybeBlockNumber
            bt <- maybeBlockTimestamp
            bi <- maybeBlockIndex
          } yield BlockInfo(bn, bt, bi)
        )
      }

  val transactionInfoRead2Columns: Read[TransactionInfo] =
    Read[(Array[Byte], String)]
      .map { case (txId, ledger) =>
        TransactionInfo(
          TransactionId.from(txId).get,
          Ledger.withNameInsensitive(ledger)
        )
      }
}
