package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.models.{DidSuffix, IdType}
import io.iohk.atala.prism.node.models.DIDService
import io.iohk.atala.prism.node.models.nodeState.{DIDServiceState, LedgerData}
import io.iohk.atala.prism.utils.syntax._

object ServicesDAO {

  /** * Get all services and its associated services endpoints that are not revoked
    * @param suffix
    * @return
    */
  def getAllActiveByDidSuffix(suffix: DidSuffix): ConnectionIO[List[DIDServiceState]] = {
    sql"""
         |SELECT s.service_id, s.id, s.did_suffix, s.type, s.service_endpoints,
         |       s.added_on_transaction_id, s.added_on, s.added_on_absn, s.added_on_osn,
         |       s.revoked_on_transaction_id, s.revoked_on, s.revoked_on_absn, s.revoked_on_osn,
         |       s.ledger,
         |FROM services AS s
         |WHERE did_suffix = $suffix AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceState].to[List]
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
         |       s.ledger,
         |FROM services AS s
         |WHERE did_suffix = $suffix AND s.id = $id AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceState].option
  }

  /** Insert service and services endpoints of that service
    * @param service
    * @param ledgerData
    * @return
    */
  def insert(service: DIDService, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val addedOn = ledgerData.timestampInfo
    val newServiceId = IdType.random

    sql"""
            |INSERT INTO services (service_id, id, did_suffix, type, service_endpoints,
            |    added_on_transaction_id, added_on, added_on_absn, added_on_osn, ledger)
            |VALUES ($newServiceId, ${service.id}, ${service.didSuffix}, ${service.`type`}, ${service.serviceEndpoints}
            |    ${ledgerData.transactionId}, ${addedOn.getAtalaBlockTimestamp.toInstant},
            |    ${addedOn.getAtalaBlockSequenceNumber}, ${addedOn.getOperationSequenceNumber},
            |    ${ledgerData.ledger})
      """.stripMargin.update.run.void
  }

  /** Revoke all services associated with the DID
    * @param suffix
    * @param ledgerData
    * @return
    */
  def revokeAllServices(suffix: DidSuffix, ledgerData: LedgerData): ConnectionIO[Boolean] = {
    val revokedOn = ledgerData.timestampInfo
    sql"""|
          |UPDATE services
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
