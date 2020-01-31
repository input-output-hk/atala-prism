package io.iohk

import io.iohk.connector.errors.{ConnectorError, SignatureVerificationError}
import io.iohk.connector.services.ConnectionsService
import io.iohk.cvp.crypto.ECKeys.toPublicKey
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.grpc.UserIdInterceptor.SignatureHeader
import io.iohk.cvp.models.ParticipantId
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither._

import scala.concurrent.{ExecutionContext, Future}

trait AuthSupport {

  def connections: ConnectionsService

  def authenticate(request: Array[Byte], signatureHeader: SignatureHeader)(
      implicit executionContext: ExecutionContext
  ): FutureEither[ConnectorError, ParticipantId] = {

      for {
        signature <- Future{Right(signatureHeader.signature)}.toFutureEither
        publicKey <- Future{Right(toPublicKey(signatureHeader.publicKey))}.toFutureEither
        _ <- Either
          .cond(
            ECSignature.verify(publicKey, request, signature),
            (),
            SignatureVerificationError()
          )
          .toFutureEither
        participantId <- connections.getParticipantId(signatureHeader.publicKey)
      } yield participantId

  }
}
