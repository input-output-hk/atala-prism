package io.iohk.atala.prism.console.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.console.models.{Contact, Institution, Statistics}

object StatisticsDAO {

  def query(institutionId: Institution.Id): ConnectionIO[Statistics] = {
    val missingConnectionStatus: Contact.ConnectionStatus = Contact.ConnectionStatus.ConnectionMissing
    val connectedConnectionStatus: Contact.ConnectionStatus = Contact.ConnectionStatus.ConnectionAccepted
    sql"""
         |SELECT
         |  (SELECT COUNT(*) FROM contacts WHERE created_by = $institutionId) AS numberOfContacts,
         |  (SELECT COUNT(*) FROM contacts WHERE created_by = $institutionId AND connection_status = $missingConnectionStatus::CONTACT_CONNECTION_STATUS_TYPE) AS numberOfContactsPendingConnection,
         |  (SELECT COUNT(*) FROM contacts WHERE created_by = $institutionId AND connection_status = $connectedConnectionStatus::CONTACT_CONNECTION_STATUS_TYPE) AS numberOfContactsConnected,
         |  (SELECT COUNT(*) FROM issuer_groups WHERE issuer_id = $institutionId) AS numberOfGroups,
         |  (SELECT COUNT(*) FROM credentials WHERE issuer_id = $institutionId) AS numberOfCredentials,
         |  (SELECT COUNT(*) FROM published_credentials JOIN credentials USING (credential_id) WHERE issuer_id = $institutionId) AS numberOfCredentialsPublished,
         |  (SELECT COUNT(*) FROM stored_credentials JOIN contacts USING (connection_id) WHERE created_by = $institutionId) AS numberOfCredentialsReceived
         |""".stripMargin.query[Statistics].unique
  }
}
