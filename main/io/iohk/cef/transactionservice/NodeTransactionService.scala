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
  * @param consensusMap The ledgers and their respective Consensus that this node supports.
  * @param txNetwork Network in charge of disseminating transactions
  * @param blockNetwork Network in charge of disseminating blocks
  * @param me information about "me" (the node this transactionservice belongs to)
  * @param txSerializable
  * @param blockSerializable
  * @param timeout
  * @param executionContext
  * @tparam State
  * @tparam Header
  */
trait NodeTransactionService[State, Tx <: Transaction[State], Q <: LedgerQuery[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]]

  def receiveBlock(blEnvelope: Envelope[Block[State, Tx]]): Future[Either[ApplicationError, Unit]]

  def getQueryService(ledgerId: LedgerId): LedgerQueryService[State, Q]

  def supportedLedgerIds: Set[LedgerId]
}
