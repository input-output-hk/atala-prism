package io.iohk.cef.transactionpool
import io.iohk.cef.ledger.{Block, Transaction}

trait TransactionPool[F[_], State] {

  def process(transaction: Transaction[State]): F[Either[TransactionPoolError, Unit]]

  def generateBlock[Header](): Block[State, Header, Transaction[State]]
}
