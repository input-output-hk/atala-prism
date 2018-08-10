package io.iohk.cef.ledger

case class LedgerStateUpdateActions[K, S](actions: Seq[LedgerStateUpdateAction[K, S]]) {
  def mapKeys[T](f: K => T): LedgerStateUpdateActions[T, S] =
    LedgerStateUpdateActions[T, S](
      actions.map {
        case InsertStateUpdate(k, v) => InsertStateUpdate(f(k), v)
        case DeleteStateUpdate(k, v) => DeleteStateUpdate(f(k), v)
        case UpdateStateUpdate(k, v) => UpdateStateUpdate(f(k), v)
      }
    )
}

sealed trait LedgerStateUpdateAction[K, S]
case class InsertStateUpdate[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
case class DeleteStateUpdate[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
case class UpdateStateUpdate[K, S](key: K, value: S) extends LedgerStateUpdateAction[K, S]
