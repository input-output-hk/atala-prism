package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.DIDServiceWithEndpoint

object ServicesDAO {
  /***
   * Get all services joined with corresponding service endpoints that are not revoked
   * @param suffix
   * @return
   */
  def getAllActiveByDidSuffix(suffix: DidSuffix): ConnectionIO[List[DIDServiceWithEndpoint]] = {
    sql"""
         |SELECT s.service_id, s.id, s.did_suffix, s.type,
         |       s.added_on_transaction_id, s.added_on, s.added_on_absn, s.added_on_osn,
         |       s.revoked_on_transaction_id, s.revoked_on, s.revoked_on_absn, s.revoked_on_osn,
         |       s.ledger,
         |       se.service_endpoint_id, se.url_index, se.url
         |FROM services AS s
         |LEFT JOIN service_endpoints se on s.service_id = se.service_id
         |WHERE did_suffix = $suffix AND s.revoked_on is NULL
       """.stripMargin.query[DIDServiceWithEndpoint].to[List]
  }

}
