package io.iohk.cef.ledger

case class LedgerStateUpdateActions[K, S](actions: Seq[LedgerStateUpdateAction[K, S]]) {
  def mapKeys[T](f: K => T): LedgerStateUpdateActions[T, S] =
    LedgerStateUpdateActions[T, S](
      actions.map {
        case InsertStateAction(k, v) => InsertStateAction(f(k), v)
        case DeleteStateAction(k, v) => DeleteStateAction(f(k), v)
        case UpdateStateAction(k, v) => UpdateStateAction(f(k), v)
      }
    )
}

sealed trait LedgerStateUpdateAction[K, S]
case class InsertStateAction[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
case class DeleteStateAction[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
case class UpdateStateAction[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
