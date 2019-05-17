package atala.obft

import atala.clock._

package object common {

  type StateSnapshot[S] = atala.state.StateSnapshot[S, TimeSlot]
  def StateSnapshot[S](s: S, t: TimeSlot): StateSnapshot[S] =
    atala.state.StateSnapshot(s, t)

  type StateGate[S] = atala.state.StateGate[S, TimeSlot]

}
