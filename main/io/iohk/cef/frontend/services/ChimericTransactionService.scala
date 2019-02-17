package io.iohk.cef.frontend.services

import io.iohk.codecs.nio.auto._
import io.iohk.crypto.sign
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models.{CreateChimericTransactionRequest, CreateSignableChimericTransactionFragment, SubmitChimericTransactionRequest}
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.chimeric._
import io.iohk.network.{Envelope, Everyone}
import io.iohk.cef.ledger.query.chimeric._
import io.iohk.codecs.nio.auto._
import io.iohk.cef.transactionservice.NodeTransactionService

import scala.concurrent.{ExecutionContext, Future}

class ChimericTransactionService(
    nodeTransactionService: NodeTransactionService[ChimericStateResult, ChimericTx, ChimericQuery]
)(
    implicit ec: ExecutionContext
) {

  def isLedgerSupported(ledgerId: LedgerId): Boolean = nodeTransactionService.supportedLedgerIds.contains(ledgerId)

  //FIXME: Hack. See CE-592
  def ledgerId: LedgerId = nodeTransactionService.supportedLedgerIds.head

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

  def executeQuery(ledgerId: LedgerId, query: ChimericQuery): Response[query.Response] = {
    Future(Right(nodeTransactionService.getQueryService(ledgerId).perform(query)))
  }
}
