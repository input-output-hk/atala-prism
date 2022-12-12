package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.Update
import io.iohk.atala.prism.models.{DidSuffix, IdType}
import io.iohk.atala.prism.node.models.DIDService
import io.iohk.atala.prism.node.models.nodeState.{DIDServiceState, DIDServiceWithEndpoint, LedgerData}
import io.iohk.atala.prism.utils.syntax._

object ServicesDAO {

  /** * Get all services and its associated services endpoints that are not revoked
    * @param suffix
    * @return
    */
  def getAllActiveByDidSuffix(suffix: DidSuffix): ConnectionIO[List[DIDServiceState]] = {
    val query = sql"""
         |SELECT s.service_id, s.id, s.did_suffix, s.type,
         |       s.added_on_transaction_id, s.added_on, s.added_on_absn, s.added_on_osn,
         |       s.revoked_on_transaction_id, s.revoked_on, s.revoked_on_absn, s.revoked_on_osn,
         |       s.ledger,
         |       se.service_endpoint_id, se.url_index, se.url
         |FROM services AS s
         |LEFT JOIN service_endpoints se on s.service_id = se.service_id
         |WHERE did_suffix = $suffix AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceWithEndpoint].to[List]

    for {
      servicesWithEndpoints <- query
      didServices = servicesWithEndpoints
        .groupBy(_.serviceId)
        .map { serviceIdAndServices =>
          val (serviceId, services) = serviceIdAndServices
          /*
           * NOTE: services.head should never fail, because services will always have at least
           *       one element inside. This is guarantied by .groupBy function that is called
           *       before .map, so groupBy will create an immutable.Map of serviceId and all
           *       corresponding services, if there is none, element would not be added to the Map
           *       TODO: find a way to represent this via type system somehow
           * */
          val service = services.head

          DIDServiceState(
            serviceId = serviceId,
            id = service.id,
            didSuffix = service.didSuffix,
            `type` = service.`type`,
            serviceEndpoints = services
              .collect { case DIDServiceWithEndpoint(_, _, _, _, Some(serviceEndpoint), _, _) =>
                serviceEndpoint
              }
              .sortBy(_.urlIndex),
            addedOn = service.addedOn,
            revokedOn = service.revokedOn
          )
        }
        .toList
    } yield didServices
  }

  /** * Get a single did service and its associated service endpoints that is not revoked
    * @param suffix
    * @param id
    * @return
    */
  def get(suffix: DidSuffix, id: String): ConnectionIO[Option[DIDServiceState]] = {
    val query = sql"""
         |SELECT s.service_id, s.id, s.did_suffix, s.type,
         |       s.added_on_transaction_id, s.added_on, s.added_on_absn, s.added_on_osn,
         |       s.revoked_on_transaction_id, s.revoked_on, s.revoked_on_absn, s.revoked_on_osn,
         |       s.ledger,
         |       se.service_endpoint_id, se.url_index, se.url
         |FROM services AS s
         |LEFT JOIN service_endpoints se on s.service_id = se.service_id
         |WHERE did_suffix = $suffix AND s.id = $id AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceWithEndpoint].to[List]

    for {
      servicesWEndpoint <- query
      service = servicesWEndpoint.headOption
      didService = service.map { s =>
        DIDServiceState(
          serviceId = s.serviceId,
          id = s.id,
          didSuffix = s.didSuffix,
          `type` = s.`type`,
          serviceEndpoints = servicesWEndpoint
            .collect { case DIDServiceWithEndpoint(_, _, _, _, Some(serviceEndpoint), _, _) =>
              serviceEndpoint
            }
            .sortBy(_.urlIndex),
          addedOn = s.addedOn,
          revokedOn = s.revokedOn
        )
      }
    } yield didService
  }

  /** Insert service and services endpoints of that service
    * @param service
    * @param ledgerData
    * @return
    */
  def insert(service: DIDService, ledgerData: LedgerData): ConnectionIO[Unit] = {
    val addedOn = ledgerData.timestampInfo
    val newServiceId = IdType.random

    val insertServiceStatement: ConnectionIO[Int] =
      sql"""
            |INSERT INTO services (service_id, id, did_suffix, type,
            |    added_on_transaction_id, added_on, added_on_absn, added_on_osn, ledger)
            |VALUES ($newServiceId, ${service.id}, ${service.didSuffix}, ${service.`type`},
            |    ${addedOn.getAtalaBlockTimestamp.toInstant}, ${addedOn.getAtalaBlockSequenceNumber},
            |    ${addedOn.getOperationSequenceNumber}, ${ledgerData.transactionId}, ${ledgerData.ledger})
            |
      """.stripMargin.update.run

    val insertServiceEndpointsStatementString =
      """|
         |INSERT INTO service_endpoints (service_endpoint_id, url_index, service_id, url)
         |VALUES (?, ?, ?, ?)
         |""".stripMargin

    val insertServiceEndpointsStatement = Update[
      (
          IdType,
          Int,
          IdType,
          String
      )
    ](insertServiceEndpointsStatementString)
      .updateMany(service.serviceEndpoints.map { endpoint =>
        (
          IdType.random, // new ID for every service endpoint
          endpoint.urlIndex,
          newServiceId, // ID of previously inserted service
          endpoint.url
        )
      })

    // compose both queries
    for {
      _ <- insertServiceStatement
      _ <- insertServiceEndpointsStatement
    } yield ()

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
