package atala.view

import atala.obft.common.StateSnapshot

// StateView is implemented around the concept of an 'actor' implemented with a
// monix stream. StateViewActorMessage is the base type of the messages processed by the
// actor.
//
sealed trait StateViewActorMessage[S]
object StateViewActorMessage {

  /** The view of the view state needs to be updated */
  case class Tick[S]() extends StateViewActorMessage[S]

  /** The a new version of the view state has been computed */
  case class StateUpdatedEvent[S](newStateSnapshot: StateSnapshot[S]) extends StateViewActorMessage[S]

  /** Perform a query on the view state, in it's own message processing slot. `action` knows
      how to perform the query */
  case class PerformQuery[S](action: () => Unit) extends StateViewActorMessage[S]

}
