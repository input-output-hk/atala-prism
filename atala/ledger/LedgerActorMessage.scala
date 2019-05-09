package atala.ledger

import atala.clock.StateSnapshot

// Ledger is implemented around the concept of an 'actor' implemented with a
// monix stream. LedgerActorMessage is the base type of the messages processed by the
// actor.
//
sealed trait LedgerActorMessage[S]
object LedgerActorMessage {

  /** The view of the ledger state needs to be updated */
  case class Tick[S]() extends LedgerActorMessage[S]

  /** The a new version of the ledger state has been computed */
  case class StateUpdatedEvent[S](newStateSnapshot: StateSnapshot[S]) extends LedgerActorMessage[S]

  /** Perform a query on the ledger state, in it's own message processing slot. `action` knows
      how to perform the query */
  case class PerformQuery[S](action: () => Unit) extends LedgerActorMessage[S]

}
