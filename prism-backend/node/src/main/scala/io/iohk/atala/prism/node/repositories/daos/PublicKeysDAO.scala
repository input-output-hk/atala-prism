package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.ECConfig
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.DIDPublicKey

object PublicKeysDAO {
  def insert(key: DIDPublicKey, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val curveName = ECConfig.CURVE_NAME
    val point = key.key.getCurvePoint

    val xBytes = point.x.toByteArray
    val yBytes = point.y.toByteArray

    val addedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, x, y,
         |   added_on, added_on_absn, added_on_osn,
         |   added_on_transaction_id, ledger)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $xBytes, $yBytes,
         |   ${addedOn.atalaBlockTimestamp}, ${addedOn.atalaBlockSequenceNumber}, ${addedOn.operationSequenceNumber},
         |   ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.update.run.void
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

  def revoke(keyId: String, ledgerData: LedgerData): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo
    sql"""
         |UPDATE public_keys
         |SET revoked_on = ${revokedOn.atalaBlockTimestamp},
         |    revoked_on_absn = ${revokedOn.atalaBlockSequenceNumber},
         |    revoked_on_osn = ${revokedOn.operationSequenceNumber},
         |    revoked_on_transaction_id = ${ledgerData.transactionId}
         |WHERE key_id = $keyId
         |""".stripMargin.update.run.map(_ > 0)
  }
}
