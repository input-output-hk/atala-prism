package io.iohk.cef.ledger

case class Block[Key,
                 Value,
                 Header <: BlockHeader,
                 Tx <: Transaction[Key, Value]](header: Header, transactions: List[Tx with Transaction[Key, Value]])
    extends (LedgerState[Key, Value] => Either[LedgerError, LedgerState[Key, Value]]) {

  override def apply(state: LedgerState[Key, Value]): Either[LedgerError, LedgerState[Key, Value]] = {
    transactions.foldLeft[Either[LedgerError, LedgerState[Key, Value]]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }

  def keys: Set[Key] = {
    transactions.foldLeft[Set[Key]](Set())(_ ++ _.keys)
  }
}
