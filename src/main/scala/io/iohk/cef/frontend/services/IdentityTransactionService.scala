package io.iohk.cef.frontend.services

import io.iohk.cef.core._
import io.iohk.cef.crypto._
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.models.IdentityTransactionRequest
import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}

import scala.concurrent.Future

class IdentityTransactionService(nodeCore: NodeCore[Set[SigningPublicKey], IdentityBlockHeader, IdentityTransaction]) {

  def process(request: IdentityTransactionRequest): Future[Either[ApplicationError, Unit]] = {
    val envelope =
      Envelope(content = request.transaction, ledgerId = request.ledgerId, NoOne)

    nodeCore.receiveTransaction(envelope)
  }
}
