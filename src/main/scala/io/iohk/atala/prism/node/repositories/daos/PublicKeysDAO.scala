package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.DIDPublicKey
import io.iohk.atala.prism.node.models.nodeState.{DIDPublicKeyState, LedgerData}
import io.iohk.atala.prism.node.utils.syntax._

import java.time.Instant

object PublicKeysDAO {
  def insert(key: DIDPublicKey, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val curveName = key.key.curveName
    val compressed = key.key.compressedKey

    val addedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO public_keys (did_suffix, key_id, key_usage, curve, compressed,
         |   added_on, added_on_absn, added_on_osn,
         |   added_on_transaction_id, ledger)
         |VALUES (${key.didSuffix}, ${key.keyId}, ${key.keyUsage}, $curveName, $compressed,
         |   ${addedOn.atalaBlockTimestamp.toInstant}, ${addedOn.atalaBlockSequenceNumber}, ${addedOn.operationSequenceNumber},
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
         |""".stripMargin.query[DIDPublicKeyState].to[List]
  }

  def listAllNonRevoked(didSuffix: DidSuffix): ConnectionIO[List[DIDPublicKeyState]] = {
    sql"""
           |SELECT did_suffix, key_id, key_usage, curve, compressed,
           |       added_on, added_on_absn, added_on_osn, added_on_transaction_id, ledger,
           |       revoked_on, revoked_on_absn, revoked_on_osn, revoked_on_transaction_id, ledger
           |FROM public_keys
           |WHERE did_suffix = $didSuffix AND revoked_on is NULL
           |""".stripMargin.query[DIDPublicKeyState].to[List]
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
        revokedOn.atalaBlockTimestamp
      )},
         |    revoked_on_absn = ${revokedOn.atalaBlockSequenceNumber},
         |    revoked_on_osn = ${revokedOn.operationSequenceNumber},
         |    revoked_on_transaction_id = ${ledgerData.transactionId}
         |WHERE did_suffix = $didSuffix AND key_id = $keyId
         |""".stripMargin.update.run.map(_ > 0)
  }

  def revokeAllKeys(
      didSuffix: DidSuffix,
      ledgerData: LedgerData
  ): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo
    sql"""
         |UPDATE public_keys
         |SET revoked_on = ${Instant.ofEpochMilli(
        revokedOn.atalaBlockTimestamp
      )},
         |    revoked_on_absn = ${revokedOn.atalaBlockSequenceNumber},
         |    revoked_on_osn = ${revokedOn.operationSequenceNumber},
         |    revoked_on_transaction_id = ${ledgerData.transactionId}
         |WHERE did_suffix = $didSuffix AND revoked_on is NULL
         |""".stripMargin.update.run.map(_ > 0)
  }
}
