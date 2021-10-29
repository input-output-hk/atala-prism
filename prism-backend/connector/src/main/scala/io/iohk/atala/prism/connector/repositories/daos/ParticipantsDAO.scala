package io.iohk.atala.prism.connector.repositories.daos

import cats.data.OptionT
import cats.implicits._
import doobie.FC
import doobie.implicits._
import io.iohk.atala.prism.connector.model.{ParticipantInfo, UpdateParticipantProfile, TokenString}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.models.ParticipantId

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): doobie.ConnectionIO[Unit] = {
    val ParticipantInfo(id, tpe, publicKey, name, did, logo, operationId) =
      participant
    sql"""
         |INSERT INTO participants (id, tpe, public_key, name, did, logo, operation_id)
         |VALUES ($id, $tpe, $publicKey, $name, $did, $logo, $operationId)
       """.stripMargin.update.run.void
  }

  def findBy(id: ParticipantId): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT id, tpe, public_key, name, did, logo, operation_id
         |FROM participants
         |WHERE id = $id
      """.stripMargin.query[ParticipantInfo].option
    }

  def findBy(
      token: TokenString
  ): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.operation_id
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByAvailableToken(
      token: TokenString
  ): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.operation_id
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token AND
         |      used_at IS NULL
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByPublicKey(
      publicKey: ECPublicKey
  ): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
           |SELECT id, tpe, public_key, name, did, logo, operation_id
           |FROM participants
         |WHERE public_key = $publicKey
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByDID(did: DID): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT id, tpe, public_key, name, did, logo, operation_id
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
         |WHERE id = $id
       """.stripMargin.update.run.flatMap { count =>
      FC.raiseError(
        new Exception(
          s"ParticipantsDAO: cannot update ParticipantProfile, update result count was not equal to 1: $count"
        )
      ).whenA(count != 1)
    }.void
  }

}
