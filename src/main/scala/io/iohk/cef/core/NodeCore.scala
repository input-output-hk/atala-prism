package io.iohk.cef.core
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

import scala.concurrent.Future

trait NodeCore[State, Header <: BlockHeader, Tx <: Transaction[State]] {

  def receiveTransaction(txEnvelope: Envelope[Tx]): Future[Either[ApplicationError, Unit]]

  def receiveBlock(blEnvelope: Envelope[Block[State, Header, Tx]]): Future[Either[ApplicationError, Unit]]
}
