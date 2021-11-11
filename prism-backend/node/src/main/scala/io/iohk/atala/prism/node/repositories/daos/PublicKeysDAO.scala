package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.models.DIDPublicKey
import io.iohk.atala.prism.utils.syntax._

import java.time.Instant

object PublicKeysDAO {
  def insert(key: DIDPublicKey, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val curveName = ECConfig.getCURVE_NAME
    val compressed = key.key.getEncodedCompressed

    val addedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, compressed,
         |   added_on, added_on_absn, added_on_osn,
         |   added_on_transaction_id, ledger)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $compressed,
         |   ${addedOn.getAtalaBlockTimestamp.toInstant}, ${addedOn.getAtalaBlockSequenceNumber}, ${addedOn.getOperationSequenceNumber},
         |   ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.update.run.void
  }

  def find(
      didSuffix: DidSuffix,
      keyId: String
  ): ConnectionIO[Option[DIDPublicKeyState]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, compressed,
         |       added_on, added_on_absn, added_on_osn, added_on_transaction_id, ledger,
         |       revoked_on, revoked_on_absn, revoked_on_osn, revoked_on_transaction_id, ledger
         |FROM public_keys
         |WHERE did_suffix = $didSuffix AND key_id = $keyId
       """.stripMargin.query[DIDPublicKeyState].option
  }

  def findAll(didSuffix: DidSuffix): ConnectionIO[List[DIDPublicKeyState]] = {
    sql"""
         |SELECT did_suffix, key_id, key_usage, curve, compressed,
         |       added_on, added_on_absn, added_on_osn, added_on_transaction_id, ledger,
         |       revoked_on, revoked_on_absn, revoked_on_osn, revoked_on_transaction_id, ledger
         |FROM public_keys
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[DIDPublicKeyState].to[List]
  }

  def revoke(
      didSuffix: DidSuffix,
      keyId: String,
      ledgerData: LedgerData
  ): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo
    sql"""
         |UPDATE public_keys
         |SET revoked_on = ${Instant.ofEpochMilli(
      revokedOn.getAtalaBlockTimestamp
    )},
         |    revoked_on_absn = ${revokedOn.getAtalaBlockSequenceNumber},
         |    revoked_on_osn = ${revokedOn.getOperationSequenceNumber},
         |    revoked_on_transaction_id = ${ledgerData.transactionId}
         |WHERE did_suffix = $didSuffix AND key_id = $keyId
         |""".stripMargin.update.run.map(_ > 0)
  }
}
