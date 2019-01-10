package io.iohk.cef.frontend.services

import io.iohk.cef.transactionservice._
import io.iohk.cef.crypto.sign
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  CreateSignableChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.ledger.chimeric.SignatureTxFragment
import io.iohk.cef.codecs.nio.auto._
import scala.concurrent.{ExecutionContext, Future}
import io.iohk.cef.ledger.chimeric.{ChimericStateResult, ChimericTx}

class ChimericTransactionService(nodeTransactionService: NodeTransactionService[ChimericStateResult, ChimericTx])(
    implicit ec: ExecutionContext
) {

  def createChimericTransaction(req: CreateChimericTransactionRequest): Response[ChimericTx] = {

    val signableFragments = req.fragments.collect {
      case signableFragments: CreateSignableChimericTransactionFragment => signableFragments
    }

    val chimericTransactionFragments = req.fragments.map(_.fragment)
    val signedTransactionFragments =
      signableFragments.map(x => SignatureTxFragment(sign(chimericTransactionFragments, x.signingPrivateKey)))

    Future(Right(ChimericTx(chimericTransactionFragments ++ signedTransactionFragments)))

  }

  def submitChimericTransaction(req: SubmitChimericTransactionRequest): Response[Unit] = {

    val chimericTx = ChimericTx(req.fragments.map(_.fragment))
    val envelope =
      Envelope(content = chimericTx, containerId = req.ledgerId, Everyone)

    nodeTransactionService.receiveTransaction(envelope)
  }

}
