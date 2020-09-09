package io.iohk.atala.prism.node.repositories

import java.time.Instant

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Get, Meta, Put, Read, Write}
import io.iohk.atala.crypto.{EC, ECConfig}
import io.iohk.atala.prism.node.bitcoin.models.Blockhash
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{CredentialId, DIDSuffix, KeyUsage, Ledger, TransactionId}
import io.iohk.atala.prism.node.operations.TimestampInfo

package object daos {
  implicit val pgKeyUsageMeta = pgEnumString[KeyUsage](
    "KEY_USAGE",
    a => KeyUsage.withNameOption(a).getOrElse(throw InvalidEnum[KeyUsage](a)),
    _.entryName
  )

  implicit val didSuffixPut: Put[DIDSuffix] = Put[String].contramap(_.suffix)
  implicit val didSuffixGet: Get[DIDSuffix] = Get[String].map(DIDSuffix(_))

  implicit val credentialIdPut: Put[CredentialId] = Put[String].contramap(_.id)
  implicit val credentialIdGet: Get[CredentialId] = Get[String].map(CredentialId(_))

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
      val curveName = ECConfig.CURVE_NAME
      val point = key.key.getCurvePoint
      (
        key.didSuffix,
        key.keyId,
        key.keyUsage,
        curveName,
        point.x.toByteArray,
        point.y.toByteArray,
        key.addedOn.atalaBlockTimestamp,
        key.addedOn.atalaBlockSequenceNumber,
        key.addedOn.operationSequenceNumber,
        key.revokedOn map (_.atalaBlockTimestamp),
        key.revokedOn map (_.atalaBlockSequenceNumber),
        key.revokedOn map (_.operationSequenceNumber)
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
          Array[Byte],
          Instant,
          Int,
          Int,
          Option[Instant],
          Option[Int],
          Option[Int]
      )
    ].map {
      case (didSuffix, keyId, keyUsage, curveId, x, y, aTimestamp, aABSN, aOSN, rTimestamp, rABSN, rOSN) =>
        assert(curveId == ECConfig.CURVE_NAME)
        val javaPublicKey = EC.toPublicKey(x, y)
        val revokeTimestampInfo = for (t <- rTimestamp; absn <- rABSN; osn <- rOSN) yield TimestampInfo(t, absn, osn)
        DIDPublicKeyState(
          didSuffix,
          keyId,
          keyUsage,
          javaPublicKey,
          TimestampInfo(aTimestamp, aABSN, aOSN),
          revokeTimestampInfo
        )
    }
  }

  implicit val blockhashPut: Put[Blockhash] = Put[Array[Byte]].contramap(_.value.toArray)

  implicit val transactionIdMeta: Meta[TransactionId] =
    Meta[Array[Byte]].timap(b =>
      TransactionId.from(b).getOrElse(throw new IllegalArgumentException("Unexpected TransactionId"))
    )(_.value.toArray)

  implicit val ledgerMeta: Meta[Ledger] =
    Meta[String].timap(b => Ledger.withNameInsensitiveOption(b).getOrElse(throw InvalidEnum[Ledger](b)))(_.entryName)
}
