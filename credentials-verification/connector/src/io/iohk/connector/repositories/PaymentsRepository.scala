package io.iohk.connector.repositories

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.connector.model.payments.{ClientNonce, Payment}
import io.iohk.connector.model.requests.CreatePaymentRequest
import io.iohk.connector.repositories.daos.PaymentsDAO
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps

import scala.concurrent.ExecutionContext

class PaymentsRepository(xa: Transactor[IO])(implicit ec: ExecutionContext) {
  def create(userId: ParticipantId, request: CreatePaymentRequest): FutureEither[Nothing, Payment] = {
    PaymentsDAO
      .create(userId, request)
      .transact(xa)
      .unsafeToFuture()
      .map(Right(_))
      .toFutureEither
  }

  def find(userId: ParticipantId, nonce: ClientNonce): FutureEither[Nothing, Option[Payment]] = {
    PaymentsDAO.find(userId, nonce).transact(xa).unsafeToFuture().map(Right(_)).toFutureEither
  }

  def find(userId: ParticipantId): FutureEither[Nothing, List[Payment]] = {
    PaymentsDAO.find(userId).transact(xa).unsafeToFuture().map(Right(_)).toFutureEither
  }
}
