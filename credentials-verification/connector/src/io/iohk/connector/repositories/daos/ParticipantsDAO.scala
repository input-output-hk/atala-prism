package io.iohk.connector.repositories.daos

import doobie.implicits._
import io.iohk.connector.model.{ParticipantInfo, TokenString}

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): doobie.ConnectionIO[Unit] = {
    val ParticipantInfo(id, tpe, name, did) = participant
    sql"""
         |INSERT INTO participants (id, tpe, name, did)
         |VALUES ($id, $tpe, $name, $did)
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(token: TokenString): doobie.ConnectionIO[Option[ParticipantInfo]] = {
    sql"""
         |SELECT p.id, p.tpe, p.name, p.did
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token
      """.stripMargin.query[ParticipantInfo].option
  }

  def findByAvailableToken(token: TokenString): doobie.ConnectionIO[Option[ParticipantInfo]] = {
    sql"""
         |SELECT p.id, p.tpe, p.name, p.did
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token AND
         |      used_at IS NULL
      """.stripMargin.query[ParticipantInfo].option
  }
}
