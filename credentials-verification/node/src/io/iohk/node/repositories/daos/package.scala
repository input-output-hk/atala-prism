package io.iohk.node.repositories

import java.time.Instant

import doobie.postgres.implicits._
import doobie.util.invariant.InvalidEnum
import doobie.util.{Get, Put, Read, Write}
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.bitcoin.models.{Blockhash, TransactionId}
import io.iohk.node.models.nodeState.DIDPublicKeyState
import io.iohk.node.models.{CredentialId, DIDSuffix, KeyUsage}
import io.iohk.node.operations.TimestampInfo

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
    Write[(DIDSuffix, String, KeyUsage, String, Array[Byte], Array[Byte],
      Instant, Int, Int, Option[Instant], Option[Int], Option[Int])].contramap { key =>
      val curveName = ECKeys.CURVE_NAME
      val point = ECKeys.getECPoint(key.key)
      (key.didSuffix, key.keyId, key.keyUsage, curveName, point.getAffineX.toByteArray, point.getAffineY.toByteArray,
        key.addedOn.atalaBlockTimestamp, key.addedOn.atalaBlockSequenceNumber, key.addedOn.operationSequenceNumber,
        key.revokedOn map (_.atalaBlockTimestamp), key.revokedOn map (_.atalaBlockSequenceNumber),
        key.revokedOn map (_.operationSequenceNumber))
    }
  }

  implicit val didPublicKeyRead: Read[DIDPublicKeyState] = {
    Read[(DIDSuffix, String, KeyUsage, String, Array[Byte], Array[Byte],
      Instant, Int, Int, Option[Instant], Option[Int], Option[Int])].map {
      case (didSuffix, keyId, keyUsage, curveId, x, y, aTimestamp, aABSN, aOSN, rTimestamp, rABSN, rOSN) =>
        assert(curveId == ECKeys.CURVE_NAME)
        val javaPublicKey = ECKeys.toPublicKey(x, y)
        val revokeTimestampInfo = for(t <- rTimestamp; absn <- rABSN; osn <- rOSN) yield TimestampInfo(t, absn, osn)
        DIDPublicKeyState(didSuffix, keyId, keyUsage, javaPublicKey,
          TimestampInfo(aTimestamp, aABSN, aOSN), revokeTimestampInfo)
    }
  }

  implicit val blockhashPut: Put[Blockhash] = Put[List[Byte]].contramap(_.toBytesBE)
  implicit val transactionIdPut: Put[TransactionId] = Put[List[Byte]].contramap(_.toBytesBE)
}
