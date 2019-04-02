package obft.clock

case class StateSnapshot[S](
    computedState: S,
    snapshotTimestamp: TimeSlot // That is, the timestamp of the last transaction this snapshot includes
)
