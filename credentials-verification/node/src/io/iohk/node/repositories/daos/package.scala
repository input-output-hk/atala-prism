package io.iohk.node.repositories

import doobie.postgres.implicits._
import doobie.util.{Get, Put, Read, Write}
import doobie.util.invariant.InvalidEnum
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.models.{CredentialId, DIDPublicKey, DIDSuffix, KeyUsage}

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

  implicit val didPublicKeyWrite: Write[DIDPublicKey] = {
    Write[(DIDSuffix, String, KeyUsage, String, Array[Byte], Array[Byte])].contramap { key =>
      val curveName = ECKeys.CURVE_NAME
      val point = ECKeys.getECPoint(key.key)
      (key.didSuffix, key.keyId, key.keyUsage, curveName, point.getAffineX.toByteArray, point.getAffineY.toByteArray)
    }
  }

  implicit val didPublicKeyRead: Read[DIDPublicKey] = {
    Read[(DIDSuffix, String, KeyUsage, String, Array[Byte], Array[Byte])].map {
      case (didSuffix, keyId, keyUsage, curveId, x, y) =>
        assert(curveId == ECKeys.CURVE_NAME)
        val javaPublicKey = ECKeys.toPublicKey(x, y)
        DIDPublicKey(didSuffix, keyId, keyUsage, javaPublicKey)
    }
  }
}
