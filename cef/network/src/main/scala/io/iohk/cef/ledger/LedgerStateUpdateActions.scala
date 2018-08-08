package io.iohk.cef.ledger

case class LedgerStateUpdateActions[K, S](actions: Seq[Action[K, S]]) {
  def mapKeys[T](f: K => T): LedgerStateUpdateActions[T, S] =
    LedgerStateUpdateActions[T, S](
      actions.map {
        case Insert(k, v) => Insert(f(k), v)
        case Delete(k, v) => Delete(f(k), v)
        case Update(k, v) => Update(f(k), v)
      }
    )
}

sealed trait Action[K, S]
case class Insert[K, S](key: K, value: S) extends Action[K, S]
case class Delete[K, S](key: K, value: S) extends Action[K, S]
case class Update[K, S](key: K, value: S) extends Action[K, S]
