package io.iohk.connector.repositories.daos

import java.util.UUID

import doobie.implicits._
import io.iohk.atala.prism.models.ParticipantId
import io.iohk.connector.model.payments.{ClientNonce, Payment}
import io.iohk.connector.model.requests.CreatePaymentRequest

object PaymentsDAO {
  def create(participantId: ParticipantId, request: CreatePaymentRequest): doobie.ConnectionIO[Payment] = {
    val id = Payment.Id(UUID.randomUUID())
    sql"""
         |INSERT INTO payments (payment_id, participant_id, nonce, amount, created_on, status, failure_reason)
         |VALUES ($id, $participantId, ${request.nonce}, ${request.amount}, NOW(), ${request.status}::PAYMENT_STATUS_TYPE, ${request.failureReason})
         |RETURNING payment_id, participant_id, nonce, amount, created_on, status, failure_reason
         |""".stripMargin.query[Payment].unique
  }

  def find(participantId: ParticipantId, nonce: ClientNonce): doobie.ConnectionIO[Option[Payment]] = {
    sql"""
         |SELECT payment_id, participant_id, nonce, amount, created_on, status, failure_reason
         |FROM payments
         |WHERE participant_id = $participantId AND
         |      nonce = $nonce
         |""".stripMargin.query[Payment].option
  }

  def find(participantId: ParticipantId): doobie.ConnectionIO[List[Payment]] = {
    sql"""
         |SELECT payment_id, participant_id, nonce, amount, created_on, status, failure_reason
         |FROM payments
         |WHERE participant_id = $participantId
         |ORDER BY created_on DESC
         |""".stripMargin.query[Payment].to[List]
  }
}
