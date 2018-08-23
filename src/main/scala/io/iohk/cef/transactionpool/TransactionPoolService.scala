package io.iohk.cef.transactionpool
import io.iohk.cef.error.ApplicationError
import io.iohk.cef.ledger.{Block, BlockHeader, Transaction}

trait TransactionPoolService[State, Header <: BlockHeader] {
  type BlockType = Block[State, Header, Transaction[State]]

  def generateBlock(): Either[ApplicationError, BlockType]

  def processTransaction(transaction: Transaction[State]): Either[ApplicationError, Unit]

  def removeBlockTransactions(block: BlockType): Either[ApplicationError, Unit]
}
