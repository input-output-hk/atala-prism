package io.iohk.cef.ledger

case class Block[State <: LedgerState](header: BlockHeader, transactions: List[Transaction[State]])
    extends (State => Either[LedgerError, State]) {

  override def apply(state: State): Either[LedgerError, State] = {
    transactions.foldLeft[Either[LedgerError, State]](Right(state))((either, tx) => {
      either.flatMap(tx(_))
    })
  }
}
