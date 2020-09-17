package io.iohk.atala.prism.cstore.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.console.models.Contact
import io.iohk.atala.prism.console.repositories.daos._
import io.iohk.atala.prism.cstore.models.StoreIndividual
import io.iohk.atala.prism.models.ParticipantId

object IndividualsDAO {

  case class IndividualCreateData(fullName: String, email: Option[String])

  def insertIndividual(verifierId: ParticipantId, data: IndividualCreateData): ConnectionIO[StoreIndividual] = {
    val individualId = ParticipantId.random()
    // For now, we create a random external id, this will be changed in a future story
    val externalId = Contact.ExternalId.random()
    val connectionStatus = Contact.ConnectionStatus.InvitationMissing
    sql"""
         |INSERT INTO contacts (created_by, contact_id, external_id, contact_data, connection_status, created_at)
         |VALUES ($verifierId, $individualId, $externalId, jsonb_build_object('full_name', ${data.fullName}, 'email', ${data.email}),
         |        ${connectionStatus: Contact.ConnectionStatus}::CONTACT_CONNECTION_STATUS_TYPE, now())
         |RETURNING contact_id, connection_status, connection_token, connection_id, contact_data ->> 'full_name', contact_data ->> 'email', created_at
       """.stripMargin.query[StoreIndividual].unique
  }

  def listIndividuals(
      createdBy: ParticipantId,
      lastSeen: Option[ParticipantId],
      limit: Int
  ): ConnectionIO[Seq[StoreIndividual]] = {
    lastSeen match {
      case Some(lastSeen) =>
        // max in CTE select is to force aggregation there is going to be at most one row
        // if it exists we want its value, otherwise default one, hence COALESCE
        sql"""
             |WITH CTE AS (
             |  SELECT COALESCE(max(created_at), to_timestamp(0)) AS last_created_at
             |  FROM contacts
             |  WHERE created_by = $createdBy AND contact_id = $lastSeen
             |)
             |
             |SELECT contact_id, connection_status, connection_token, connection_id, contact_data ->> 'full_name', contact_data ->> 'email', created_at
             |FROM CTE CROSS JOIN contacts
             |WHERE created_by = $createdBy AND (created_at, contact_id) > (last_created_at, $lastSeen)
             |ORDER BY (created_at, contact_id) ASC
             |LIMIT $limit
       """.stripMargin.query[StoreIndividual].to[Seq]
      case None =>
        sql"""
             |SELECT contact_id, connection_status, connection_token, connection_id, contact_data ->> 'full_name', contact_data ->> 'email', created_at
             |FROM contacts
             |WHERE created_by = $createdBy
             |ORDER BY (created_at, contact_id) ASC
             |LIMIT $limit
           """.stripMargin.query[StoreIndividual].to[Seq]
    }
  }
}
