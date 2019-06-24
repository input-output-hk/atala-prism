package atala.state

case class StateSnapshot[S, T](
    computedState: S,
    snapshotTimestamp: T // That is, the timestamp of the last transaction this snapshot includes
)
