package io.iohk.atala.prism.management.console.repositories.daos

import doobie.implicits.toSqlInterpolator
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.management.console.models.ParticipantId

object RequestNoncesDAO {
  def burn(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): doobie.ConnectionIO[Unit] = {
    sql"""
         |INSERT INTO request_nonces (request_nonce, participant_id)
         |VALUES ($requestNonce, $participantId)
         |ON CONFLICT DO NOTHING
         |RETURNING request_nonce
         |""".stripMargin.query[RequestNonce].option.map {
      case Some(_) => ()
      case None    => throw new RuntimeException("The nonce was already used")
    }
  }

  def available(
      participantId: ParticipantId,
      requestNonce: RequestNonce
  ): doobie.ConnectionIO[Boolean] = {
    sql"""
         |SELECT request_nonce
         |FROM request_nonces
         |WHERE request_nonce = $requestNonce AND
         |      participant_id = $participantId
         |""".stripMargin.query[RequestNonce].option.map(_.isEmpty)
  }
}
