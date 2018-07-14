package io.iohk.cef.ledger

case class Block[State <: LedgerState[Key, _],
                 Key,
                 Header <: BlockHeader,
                 Tx <: Transaction[State, Key]](header: Header, transactions: List[Tx with Transaction[State, Key]])
    extends (State => Either[LedgerError, State]) {

  override def apply(state: State): Either[LedgerError, State] = {
    transactions.foldLeft[Either[LedgerError, State]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }

  def keys: Set[Key] = {
    transactions.foldLeft[Set[Key]](Set())(_ ++ _.keys)
  }
}
