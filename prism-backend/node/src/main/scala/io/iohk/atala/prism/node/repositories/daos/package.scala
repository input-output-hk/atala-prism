package io.iohk.atala.prism.node.repositories

import cats.data.NonEmptyList

import java.time.Instant
import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.{Get, Meta, Put, Read, Write}
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.{EC, MerkleRoot, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.kotlin.identity.DIDSuffix
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.nodeState.{CredentialBatchState, DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

import scala.collection.compat.immutable.ArraySeq

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
          .getOrElse(throw InvalidEnum[AtalaObjectTransactionSubmissionStatus](a)),
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

  implicit val didSuffixPut: Put[DIDSuffix] = Put[String].contramap(_.getValue)
  implicit val didSuffixGet: Get[DIDSuffix] = Get[String].map(DIDSuffix.fromString)

  implicit val credentialIdPut: Put[CredentialId] = Put[String].contramap(_.id)
  implicit val credentialIdGet: Get[CredentialId] = Get[String].map(CredentialId(_))

  implicit val credentialBatchIdMeta: Meta[CredentialBatchId] =
    Meta[String].timap(CredentialBatchId.fromString)(_.getId)

  implicit val didPublicKeyWrite: Write[DIDPublicKeyState] = {
    Write[
      (
          DIDSuffix,
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
        Instant.ofEpochMilli(key.addedOn.timestampInfo.getAtalaBlockTimestamp),
        key.addedOn.timestampInfo.getAtalaBlockSequenceNumber,
        key.addedOn.timestampInfo.getOperationSequenceNumber,
        key.revokedOn map (x => Instant.ofEpochMilli(x.timestampInfo.getAtalaBlockTimestamp)),
        key.revokedOn map (_.timestampInfo.getAtalaBlockSequenceNumber),
        key.revokedOn map (_.timestampInfo.getOperationSequenceNumber)
      )
    }
  }

  implicit val didPublicKeyRead: Read[DIDPublicKeyState] = {
    Read[
      (
          DIDSuffix,
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
        val javaPublicKey: ECPublicKey = EC.toPublicKeyFromCompressed(compressed)
        val revokeLedgerData =
          for (transactionId <- rTransactionId; ledger <- rLedger; t <- rTimestamp; absn <- rABSN; osn <- rOSN)
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
    Meta[Array[Byte]].timap(value => AtalaObjectId(value.toVector))(_.value.toArray)

  implicit val atalaOperationInfoRead: Read[AtalaOperationInfo] = {
    Read[
      (
          AtalaOperationId,
          AtalaObjectId,
          AtalaOperationStatus,
          Option[AtalaObjectTransactionSubmissionStatus],
          Option[TransactionId]
      )
    ].map(AtalaOperationInfo.tupled)
  }

  implicit val atalaObjectRead: Read[AtalaObjectInfo] = {
    Read[
      (
          AtalaObjectId,
          Array[Byte],
          Boolean,
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
            processed,
            maybeTransactionId,
            maybeLedger,
            maybeBlockNumber,
            maybeBlockTimestamp,
            maybeBlockIndex
          ) =>
        AtalaObjectInfo(
          objectId,
          byteContent,
          processed,
          (maybeTransactionId, maybeLedger, maybeBlockNumber, maybeBlockTimestamp, maybeBlockIndex) match {
            case (Some(transactionId), Some(ledger), Some(blockNumber), Some(blockTimestamp), Some(blockIndex)) =>
              Some(TransactionInfo(transactionId, ledger, Some(BlockInfo(blockNumber, blockTimestamp, blockIndex))))
            case _ => None
          }
        )
    }
  }

  implicit val ledgerDataOptionGet: Get[LedgerData] =
    Get.Advanced
      .other[(ArraySeq[Byte], String, Instant, Int, Int)](
        NonEmptyList.of("TRANSACTION_ID", "VARCHAR(32)", "TIMESTAMPTZ", "INTEGER", "INTEGER")
      )
      .tmap {
        case (tId, ledger, abt, absn, osn) =>
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
              case (Some(rTrId), Some(rLedger), Some(rAbt), Some(rAbsn), Some(rOsn)) =>
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
            DIDSuffix.fromString(suffix),
            new MerkleRoot(SHA256Digest.fromBytes(root)),
            issuedOn,
            revokedOn,
            SHA256Digest.fromBytes(sha)
          )
      }
  }

}
