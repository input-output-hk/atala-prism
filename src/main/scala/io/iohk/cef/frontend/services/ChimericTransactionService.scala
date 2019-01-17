package io.iohk.cef.frontend.services

import io.iohk.cef.transactionservice._
import io.iohk.cef.crypto.sign
import io.iohk.cef.frontend.client.Response
import io.iohk.cef.frontend.models.{
  CreateChimericTransactionRequest,
  CreateSignableChimericTransactionFragment,
  SubmitChimericTransactionRequest
}
import io.iohk.cef.ledger.chimeric.{
  TxOutRef,
  UtxoResult,
  AddressResult,
  NonceResult,
  SignatureTxFragment,
  CurrencyQuery
}
import io.iohk.cef.query.ledger.chimeric._

import io.iohk.cef.codecs.nio.auto._
import scala.concurrent.{ExecutionContext, Future}
import io.iohk.cef.ledger.chimeric.{ChimericStateResult, ChimericTx}

class ChimericTransactionService(
    nodeTransactionService: NodeTransactionService[ChimericStateResult, ChimericTx],
    queryService: ChimericQueryService
)(
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

  def queryCreatedCurrency(currency: String): Response[Option[CurrencyQuery]] =
    Future.successful(
      Right(
        queryService.perform(ChimericQuery.CreatedCurrency(currency))
      )
    )

  def queryUtxoBalance(txOutRef: TxOutRef): Response[Option[UtxoResult]] =
    Future.successful(
      Right(
        queryService.perform(ChimericQuery.UtxoBalance(txOutRef))
      )
    )

  def queryAddressBalance(address: String): Response[Option[AddressResult]] =
    Future.successful(
      Right(
        queryService.perform(ChimericQuery.AddressBalance(address))
      )
    )

  def queryAddressNonce(address: String): Response[Option[NonceResult]] =
    Future.successful(
      Right(
        queryService.perform(ChimericQuery.AddressNonce(address))
      )
    )

}
