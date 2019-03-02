package io.iohk.cef.transactionservice

import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.query.{LedgerQuery, LedgerQueryService}
import io.iohk.cef.ledger.{Block, LedgerId, Transaction}
import io.iohk.network.Envelope

import scala.concurrent.Future

/**
  * A NodeTransactionService orchestrates the interaction between the frontend, network, transaction pool and the consensus.
  * Important note: Currently it is assumed that the network is fully connected. If this assumption does not hold,
  * the NodeTransactionService's dissemination will not reach all nodes.
  */
trait NodeTransactionService[State, Tx <: Transaction[State], Q <: LedgerQuery[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]]

  def receiveBlock(blEnvelope: Envelope[Block[State, Tx]]): Future[Either[ApplicationError, Unit]]

  def getQueryService(ledgerId: LedgerId): LedgerQueryService[State, Q]

  def supportedLedgerIds: Set[LedgerId]
}
