package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.node.models.{DidSuffix, IdType}
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.utils.syntax._

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
            |    ${ledgerData.transactionId}, ${addedOn.atalaBlockTimestamp.toInstant},
            |    ${addedOn.atalaBlockSequenceNumber}, ${addedOn.operationSequenceNumber})
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
          |SET revoked_on = ${revokedOn.atalaBlockTimestamp.toInstant},
          |    revoked_on_absn = ${revokedOn.atalaBlockSequenceNumber},
          |    revoked_on_osn = ${revokedOn.operationSequenceNumber},
          |    revoked_on_transaction_id = ${ledgerData.transactionId}
          |WHERE did_suffix = $suffix AND revoked_on is NULL
          |""".stripMargin.update.run.map(_ > 0)

  }

}
