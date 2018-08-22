package io.iohk.cef.txpool
import io.iohk.cef.ledger.{Block, Transaction}

trait TxPool[F[_], State] {

  def process(transaction: Transaction[State]): F[Either[TxPoolError, Unit]]

  def generateBlock[Header](): Block[State, Header, Transaction[State]]
}
