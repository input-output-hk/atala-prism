package io.iohk.atala.prism.management.console.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.management.console.models.{ParticipantId, Statistics, TimeInterval}

object StatisticsDAO {
  def query(
      participantId: ParticipantId,
      timeIntervalMaybe: Option[TimeInterval]
  ): ConnectionIO[Statistics] = {
    val sqlQuery = timeIntervalMaybe match {
      case Some(TimeInterval(start, end)) =>
        sql"""
             |SELECT
             |  (SELECT COUNT(*) FROM contacts
             |   WHERE created_by = $participantId AND created_at BETWEEN $start AND $end
             |  ) AS numberOfContacts,
             |  (SELECT COUNT(*) FROM institution_groups
             |   WHERE institution_id = $participantId AND created_at BETWEEN $start AND $end
             |  ) AS numberOfGroups,
             |  (SELECT COUNT(*) FROM draft_credentials
             |   WHERE issuer_id = $participantId AND created_at BETWEEN $start AND $end
             |  ) AS numberOfCredentials,
             |  (SELECT COUNT(*) FROM published_credentials
             |   JOIN draft_credentials USING (credential_id)
             |   JOIN published_batches USING (batch_id)
             |   WHERE issuer_id = $participantId AND stored_at BETWEEN $start AND $end
             |  ) AS numberOfCredentialsPublished,
             |  (SELECT COUNT(*) FROM received_credentials
             |   JOIN contacts USING (contact_id)
             |   WHERE created_by = $participantId AND received_at BETWEEN $start AND $end
             |  ) AS numberOfCredentialsReceived
             |"""
      case None =>
        sql"""
             |SELECT
             |  (SELECT COUNT(*) FROM contacts WHERE created_by = $participantId) AS numberOfContacts,
             |  (SELECT COUNT(*) FROM institution_groups WHERE institution_id = $participantId) AS numberOfGroups,
             |  (SELECT COUNT(*) FROM draft_credentials WHERE issuer_id = $participantId) AS numberOfCredentials,
             |  (SELECT COUNT(*) FROM published_credentials JOIN draft_credentials USING (credential_id) WHERE issuer_id = $participantId) AS numberOfCredentialsPublished,
             |  (SELECT COUNT(*) FROM received_credentials JOIN contacts USING (contact_id) WHERE created_by = $participantId) AS numberOfCredentialsReceived
             |"""
    }
    sqlQuery.stripMargin.query[Statistics].unique
  }
}
