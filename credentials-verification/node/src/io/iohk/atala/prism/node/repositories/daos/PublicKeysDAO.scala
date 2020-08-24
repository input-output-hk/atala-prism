package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.crypto.ECConfig
import io.iohk.atala.prism.node.models.nodeState.DIDPublicKeyState
import io.iohk.atala.prism.node.models.{DIDPublicKey, DIDSuffix}
import io.iohk.atala.prism.node.operations.TimestampInfo

object PublicKeysDAO {
  def insert(key: DIDPublicKey, timestampInfo: TimestampInfo): ConnectionIO[Unit] = {
    val curveName = ECConfig.CURVE_NAME
    val point = key.key.getCurvePoint

    val xBytes = point.x.toByteArray
    val yBytes = point.y.toByteArray

    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, x, y, added_on, added_on_absn, added_on_osn)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $xBytes, $yBytes, ${timestampInfo.atalaBlockTimestamp}, ${timestampInfo.atalaBlockSequenceNumber}, ${timestampInfo.operationSequenceNumber})
       """.stripMargin.update.run.map(_ => ())
  }

  def find(didSuffix: DIDSuffix, keyId: String): ConnectionIO[Option[DIDPublicKeyState]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, x, y, added_on, added_on_absn, added_on_osn, revoked_on, revoked_on_absn, revoked_on_osn
         |FROM public_keys
         |WHERE did_suffix = $didSuffix AND key_id = $keyId
       """.stripMargin.query[DIDPublicKeyState].option
  }

  def findAll(didSuffix: DIDSuffix): ConnectionIO[List[DIDPublicKeyState]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, x, y, added_on, added_on_absn, added_on_osn, revoked_on, revoked_on_absn, revoked_on_osn
         |FROM public_keys
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[DIDPublicKeyState].to[List]
  }

  def revoke(keyId: String, timestampInfo: TimestampInfo): ConnectionIO[Boolean] = {
    sql"""
         |UPDATE public_keys
         |SET revoked_on = ${timestampInfo.atalaBlockTimestamp}, revoked_on_absn = ${timestampInfo.atalaBlockSequenceNumber}, revoked_on_osn = ${timestampInfo.operationSequenceNumber}
         |WHERE key_id = $keyId
         |""".stripMargin.update.run.map(_ > 0)
  }
}
