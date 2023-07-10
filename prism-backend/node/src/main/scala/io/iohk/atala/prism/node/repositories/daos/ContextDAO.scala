package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.{DidSuffix, IdType}
import io.iohk.atala.prism.node.models.nodeState.{DIDServiceState, LedgerData}
import io.iohk.atala.prism.utils.syntax._

object ContextDAO {

  /** * Get all context strings that are not revoked
    * @param suffix
    * @return
    */
  def getAllActiveByDidSuffix(suffix: DidSuffix): ConnectionIO[List[String]] = {
    sql"""
         |SELECT context
         |FROM contexts
         |WHERE did_suffix = $suffix AND revoked_on is NULL
       """.stripMargin.query[String].to[List]
  }

  /** * Get a single did service and its associated service endpoints that is not revoked
    * @param suffix
    * @param id
    * @return
    */
  def get(suffix: DidSuffix, id: String): ConnectionIO[Option[DIDServiceState]] = {
    sql"""
         |SELECT s.service_id, s.id, s.did_suffix, s.type, s.service_endpoints,
         |       s.added_on_transaction_id, s.added_on, s.added_on_absn, s.added_on_osn,
         |       s.revoked_on_transaction_id, s.revoked_on, s.revoked_on_absn, s.revoked_on_osn,
         |       s.ledger
         |FROM services AS s
         |WHERE did_suffix = $suffix AND s.id = $id AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceState].option
  }

  /** Insert context string and its associated didSuffix
    * @param contextStr
    * @param didSuffix
    * @param ledgerData
    * @return
    */
  def insert(contextStr: String, didSuffix: DidSuffix, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val addedOn = ledgerData.timestampInfo
    val newContextId = IdType.random

    sql"""
            |INSERT INTO contexts (context_id, did_suffix, context,
            |    added_on_transaction_id, added_on, added_on_absn, added_on_osn)
            |VALUES ($newContextId, $didSuffix, $contextStr,
            |    ${ledgerData.transactionId}, ${addedOn.getAtalaBlockTimestamp.toInstant},
            |    ${addedOn.getAtalaBlockSequenceNumber}, ${addedOn.getOperationSequenceNumber})
      """.stripMargin.update.run.void
  }

  /** Revoke all context strings associated with the DID
    * @param suffix
    * @param ledgerData
    * @return
    */
  def revokeAllContextStrings(suffix: DidSuffix, ledgerData: LedgerData): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo
    sql"""|
          |UPDATE contexts
          |SET revoked_on = ${revokedOn.getAtalaBlockTimestamp.toInstant},
          |    revoked_on_absn = ${revokedOn.getAtalaBlockSequenceNumber},
          |    revoked_on_osn = ${revokedOn.getOperationSequenceNumber},
          |    revoked_on_transaction_id = ${ledgerData.transactionId}
          |WHERE did_suffix = $suffix AND revoked_on is NULL
          |""".stripMargin.update.run.map(_ > 0)

  }

  /** Revoke single service of a DID
    * @param suffix
    * @param id
    * @param ledgerData
    * @return
    */
  def revokeService(suffix: DidSuffix, id: String, ledgerData: LedgerData): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo

    sql"""|
          |UPDATE services
          |SET revoked_on = ${revokedOn.getAtalaBlockTimestamp.toInstant},
          |    revoked_on_absn = ${revokedOn.getAtalaBlockSequenceNumber},
          |    revoked_on_osn = ${revokedOn.getOperationSequenceNumber},
          |    revoked_on_transaction_id = ${ledgerData.transactionId}
          |WHERE did_suffix = $suffix AND id = $id AND revoked_on is NULL
          |""".stripMargin.update.run.map(_ > 0)
  }

}
