package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.RaftConsensus.RaftContext
import io.iohk.cef.consensus.raft.RaftFSM.Transition

sealed trait NodeEvent

case object ElectionTimeout extends NodeEvent
case object MajorityVoteReceived extends NodeEvent
case object NodeWithHigherTermDiscovered extends NodeEvent
case object LeaderDiscovered extends NodeEvent

trait StateCode

object RaftFSM {
  type Transition[Command] = (RaftContext[Command], NodeEvent) => RaftContext[Command]
}

class RaftFSM[Command](
    becomeFollower: Transition[Command],
    becomeCandidate: Transition[Command],
    becomeLeader: Transition[Command]) {

  private val identity: Transition[Command] = (rc, _) => rc

  private val keyFn: RaftContext[Command] => StateCode = rc => rc.role.stateCode

  private val followerState: Transition[Command] = eventCata(electionTimeout = becomeCandidate)

  private val candidateState: Transition[Command] =
    eventCata(electionTimeout = becomeCandidate, majorityVoteReceived = becomeLeader, leaderDiscovered = becomeFollower)

  private val leaderState: Transition[Command] = eventCata(nodeWithHigherTermDiscovered = becomeFollower)

  private val table = Map[StateCode, Transition[Command]](
    Follower -> followerState,
    Candidate -> candidateState,
    Leader -> leaderState
  )

  private def eventCata(
      electionTimeout: Transition[Command] = identity,
      majorityVoteReceived: Transition[Command] = identity,
      nodeWithHigherTermDiscovered: Transition[Command] = identity,
      leaderDiscovered: Transition[Command] = identity)(rc: RaftContext[Command], e: NodeEvent): RaftContext[Command] =
    e match {
      case ElectionTimeout =>
        electionTimeout(rc, e)
      case MajorityVoteReceived =>
        majorityVoteReceived(rc, e)
      case NodeWithHigherTermDiscovered =>
        nodeWithHigherTermDiscovered(rc, e)
      case LeaderDiscovered =>
        leaderDiscovered(rc, e)
    }

  def apply(rc: RaftContext[Command], e: NodeEvent): RaftContext[Command] =
    table(keyFn(rc)).apply(rc, e)
}
