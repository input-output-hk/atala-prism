package io.iohk.atala.prism.management.console.repositories.daos

import cats.implicits._
import doobie.implicits.{toSqlInterpolator, _}
import doobie.{ConnectionIO, FC}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.management.console.models.{ParticipantId, ParticipantInfo, UpdateParticipantProfile}
import doobie.implicits.legacy.instant._

import java.time.Instant

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): ConnectionIO[Unit] = {
    val ParticipantInfo(id, name, did, logo) = participant
    val createdAt = Instant.now()
    sql"""
         |INSERT INTO participants (participant_id, name, did, logo, created_at)
         |VALUES ($id, $name, $did, $logo, $createdAt)
       """.stripMargin.update.run.void
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

  def updateParticipantByID(
      id: ParticipantId,
      profile: UpdateParticipantProfile
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |UPDATE participants
         |SET logo = ${profile.logo},
         |name = ${profile.name}
         |WHERE participant_id = $id
       """.stripMargin.update.run.flatMap { count =>
      FC.raiseError(
        new Exception(
          s"ParticipantsDAO: cannot update ParticipantProfile, update result count was not equal to 1: $count"
        )
      ).whenA(count != 1)
    }.void
  }
}
