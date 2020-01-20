package io.iohk.connector.repositories.daos

import cats.data.OptionT
import doobie.implicits._
import io.iohk.connector.model.{ParticipantInfo, TokenString}
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import io.iohk.cvp.models.ParticipantId

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): doobie.ConnectionIO[Unit] = {
    val ParticipantInfo(id, tpe, publicKey, name, did, logo) = participant
    sql"""
         |INSERT INTO participants (id, tpe, public_key, name, did, logo)
         |VALUES ($id, $tpe, $publicKey, $name, $did, $logo)
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(id: ParticipantId): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT id, tpe, public_key ,name, did, logo
         |FROM participants
         |WHERE id = $id
      """.stripMargin.query[ParticipantInfo].option
  }

  def findBy(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token
      """.stripMargin.query[ParticipantInfo].option
  }

  def findByAvailableToken(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token AND
         |      used_at IS NULL
      """.stripMargin.query[ParticipantInfo].option
  }

  def findByPublicKey(publicKey: EncodedPublicKey): OptionT[doobie.ConnectionIO, ParticipantInfo] = OptionT {
    sql"""
         |SELECT id, tpe, public_key, name, did, logo
         |FROM participants
         |WHERE public_key = $publicKey
      """.stripMargin.query[ParticipantInfo].option
  }

}
