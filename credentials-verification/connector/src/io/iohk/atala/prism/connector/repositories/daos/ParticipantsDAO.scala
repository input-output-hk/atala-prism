package io.iohk.atala.prism.connector.repositories.daos

import cats.data.OptionT
import doobie.implicits._
import io.iohk.atala.crypto.ECPublicKey
import io.iohk.atala.prism.connector.model.{ParticipantInfo, TokenString}
import io.iohk.atala.prism.models.DoobieImplicits._
import io.iohk.atala.prism.models.ParticipantId

object ParticipantsDAO {
  def insert(participant: ParticipantInfo): doobie.ConnectionIO[Unit] = {
    val ParticipantInfo(id, tpe, publicKey, name, did, logo, transactionId, ledger) = participant
    sql"""
         |INSERT INTO participants (id, tpe, public_key, name, did, logo, transaction_id, ledger)
         |VALUES ($id, $tpe, $publicKey, $name, $did, $logo, $transactionId, $ledger)
       """.stripMargin.update.run.map(_ => ())
  }

  def findBy(id: ParticipantId): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT id, tpe, public_key, name, did, logo, transaction_id, ledger
         |FROM participants
         |WHERE id = $id
      """.stripMargin.query[ParticipantInfo].option
    }

  def findBy(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.transaction_id, p.ledger
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByAvailableToken(token: TokenString): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT p.id, p.tpe, p.public_key, p.name, p.did, p.logo, p.transaction_id, p.ledger
         |FROM connection_tokens t
         |JOIN participants p ON p.id = t.initiator
         |WHERE t.token = $token AND
         |      used_at IS NULL
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByPublicKey(publicKey: ECPublicKey): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT id, tpe, public_key, name, did, logo, transaction_id, ledger
         |FROM participants
         |WHERE public_key = $publicKey
      """.stripMargin.query[ParticipantInfo].option
    }

  def findByDID(did: String): OptionT[doobie.ConnectionIO, ParticipantInfo] =
    OptionT {
      sql"""
         |SELECT id, tpe, public_key, name, did, logo, transaction_id, ledger
         |FROM participants
         |WHERE did = $did
      """.stripMargin.query[ParticipantInfo].option
    }
}
