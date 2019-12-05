package io.iohk.connector.repositories.daos

import cats.data.OptionT
import doobie.implicits._
import io.iohk.connector.model.{ECPublicKey, ParticipantInfo, TokenString}
import io.iohk.cvp.models.ParticipantId

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): doobie.ConnectionIO[Unit] = {
    val ParticipantInfo(id, tpe, name, did, logo) = participant
    sql"""
         |INSERT INTO participants (id, tpe, name, did, logo)
         |VALUES ($id, $tpe, $name, $did, $logo)
       """.stripMargin.update.run.map(_ => ())
  }

  def insertPublicKey(holderId: ParticipantId, publicKey: ECPublicKey): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO holder_public_keys (participant_id, x, y)
         |VALUES ($holderId, ${publicKey.x}, ${publicKey.y})
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT p.id, p.tpe, p.name, p.did, p.logo
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token
      """.stripMargin.query[ParticipantInfo].option
  }

  def findByAvailableToken(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT p.id, p.tpe, p.name, p.did, p.logo
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token AND
         |      used_at IS NULL
      """.stripMargin.query[ParticipantInfo].option
  }

  def findPublicKey(id: ParticipantId): OptionT[doobie.ConnectionIO, ECPublicKey] = OptionT {
    sql"""
         |SELECT x, y
         |FROM holder_public_keys
         |WHERE participant_id = $id
      """.stripMargin.query[ECPublicKey].option
  }
}
