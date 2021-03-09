package io.iohk.atala.prism.management.console.repositories.daos

import java.time.Instant

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.implicits.legacy.instant._
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo}

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): ConnectionIO[Unit] = {
    val ParticipantInfo(id, name, did, logo) = participant
    val createdAt = Instant.now()
    sql"""
         |INSERT INTO participants (participant_id, name, did, logo, created_at)
         |VALUES ($id, $name, $did, $logo, $createdAt)
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(id: ParticipantId): ConnectionIO[Option[ParticipantInfo]] = {
    sql"""
         |SELECT participant_id, name, did, logo, created_at
         |FROM participants
         |WHERE participant_id = $id
      """.stripMargin.query[ParticipantInfo].option
  }

  def findByDID(did: DID): ConnectionIO[Option[ParticipantInfo]] = {
    sql"""
         |SELECT participant_id, name, did, logo, created_at
         |FROM participants
         |WHERE did = $did
      """.stripMargin.query[ParticipantInfo].option
  }
}
