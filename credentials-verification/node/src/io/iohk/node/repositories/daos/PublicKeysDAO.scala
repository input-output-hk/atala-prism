package io.iohk.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.models.{DIDPublicKey, DIDSuffix}

object PublicKeysDAO {
  def insert(key: DIDPublicKey): ConnectionIO[Unit] = {
    val curveName = ECKeys.CURVE_NAME
    val point = ECKeys.getECPoint(key.key)

    val xBytes = point.getAffineX.toByteArray
    val yBytes = point.getAffineY.toByteArray

    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, x, y)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $xBytes, $yBytes)
       """.stripMargin.update.run.map(_ => ())
  }

  def find(didSuffix: DIDSuffix, keyId: String): ConnectionIO[Option[DIDPublicKey]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, x, y
         |FROM public_keys
         |WHERE did_suffix = $didSuffix AND key_id = $keyId
       """.stripMargin.query[DIDPublicKey].option
  }

  def findAll(didSuffix: DIDSuffix): ConnectionIO[List[DIDPublicKey]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, x, y
         |FROM public_keys
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[DIDPublicKey].to[List]
  }
}
