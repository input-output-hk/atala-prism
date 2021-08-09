package io.iohk.atala.prism.node.repositories

import java.time.Instant
import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.{Get, Meta, Put, Read, Write}
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.credentials.{CredentialBatchId, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.daos.BaseDAO
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionId, TransactionInfo}
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmissionStatus,
  AtalaOperationInfo,
  AtalaOperationStatus,
  CredentialId,
  KeyUsage
}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey

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

  implicit val didSuffixPut: Put[DIDSuffix] = Put[String].contramap(_.value)
  implicit val didSuffixGet: Get[DIDSuffix] = Get[String].map(DIDSuffix.unsafeFromString)

  implicit val credentialIdPut: Put[CredentialId] = Put[String].contramap(_.id)
  implicit val credentialIdGet: Get[CredentialId] = Get[String].map(CredentialId(_))

  implicit val credentialBatchIdMeta: Meta[CredentialBatchId] =
    Meta[String].timap(CredentialBatchId.fromString(_).get)(_.id)

  implicit val didPublicKeyWrite: Write[DIDPublicKeyState] = {
    Write[
      (
          DIDSuffix,
          String,
          KeyUsage,
          String,
          Array[Byte],
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
      val point = key.key.getCurvePoint
      (
        key.didSuffix,
        key.keyId,
        key.keyUsage,
        curveName,
        point.getX.bytes(),
        point.getY.bytes(),
        key.addedOn.timestampInfo.atalaBlockTimestamp,
        key.addedOn.timestampInfo.atalaBlockSequenceNumber,
        key.addedOn.timestampInfo.operationSequenceNumber,
        key.revokedOn map (_.timestampInfo.atalaBlockTimestamp),
        key.revokedOn map (_.timestampInfo.atalaBlockSequenceNumber),
        key.revokedOn map (_.timestampInfo.operationSequenceNumber)
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
              timestampInfo = TimestampInfo(t, absn, osn)
            )
        DIDPublicKeyState(
          didSuffix,
          keyId,
          keyUsage,
          javaPublicKey,
          LedgerData(
            transactionId = aTransactionId,
            ledger = aLedger,
            timestampInfo = TimestampInfo(aTimestamp, aABSN, aOSN)
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
}
