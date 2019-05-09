package atala.ledger

import atala.clock._

class StateStorage[S](defaultState: S) {
  private var store: StateSnapshot[S] = StateSnapshot(defaultState, TimeSlot.zero)
  def put(state: StateSnapshot[S]): Unit = store = state
  def currentState: StateSnapshot[S] = store
}
object StateStorage {
  def apply[S](initialState: S): StateStorage[S] = new StateStorage[S](initialState)
}
