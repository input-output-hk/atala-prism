package io.iohk.cef.frontend.services

import io.iohk.cef.core.{Envelope, Everyone, NodeCore}
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.frontend.models.ChimericTransactionRequest
import io.iohk.cef.ledger.chimeric.{ChimericBlockHeader, ChimericStateValue, ChimericTx}

import scala.concurrent.Future

class ChimericTransactionService(nodeCore: NodeCore[ChimericStateValue, ChimericBlockHeader, ChimericTx]) {

  def process(request: ChimericTransactionRequest): Future[Either[ApplicationError, Unit]] = {
    val envelope: Envelope[ChimericTx] =
      Envelope(content = request.transaction, ledgerId = request.ledgerId, Everyone)

    nodeCore.receiveTransaction(envelope)
  }
}
