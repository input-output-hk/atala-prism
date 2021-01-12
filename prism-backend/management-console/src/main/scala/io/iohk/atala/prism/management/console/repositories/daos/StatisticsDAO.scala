package io.iohk.atala.prism.management.console.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics}

object StatisticsDAO {
  def query(participantId: ParticipantId): ConnectionIO[Statistics] = {
    sql"""
         |SELECT
         |  (SELECT COUNT(*) FROM contacts WHERE created_by = $participantId) AS numberOfContacts,
         |  (SELECT COUNT(*) FROM institution_groups WHERE institution_id = $participantId) AS numberOfGroups,
         |  (SELECT COUNT(*) FROM draft_credentials WHERE issuer_id = $participantId) AS numberOfCredentials,
         |  (SELECT COUNT(*) FROM published_credentials JOIN draft_credentials USING (credential_id) WHERE issuer_id = $participantId) AS numberOfCredentialsPublished,
         |  (SELECT COUNT(*) FROM received_credentials JOIN contacts USING (contact_id) WHERE created_by = $participantId) AS numberOfCredentialsReceived
         |""".stripMargin.query[Statistics].unique
  }
}
