package io.iohk.cef.ledger

case class Block[State <: LedgerState[Key, _], Key](header: BlockHeader, transactions: List[Transaction[State, Key]])
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
