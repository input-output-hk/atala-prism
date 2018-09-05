package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.RaftConsensus.RaftState
import io.iohk.cef.consensus.raft.RaftFSM.Transition

sealed trait NodeEvent

case object ElectionTimeout extends NodeEvent
case object MajorityVoteReceived extends NodeEvent
case object NodeWithHigherTermDiscovered extends NodeEvent
case object LeaderDiscovered extends NodeEvent

trait StateCode

object RaftFSM {
  type Transition[Command] = (RaftState[Command], NodeEvent) => RaftState[Command]
}

class RaftFSM[Command](
    becomeFollower: Transition[Command],
    becomeCandidate: Transition[Command],
    becomeLeader: Transition[Command]) {

  private val identity: Transition[Command] = (rc, _) => rc

  private val keyFn: RaftState[Command] => StateCode = rc => rc.role.stateCode

  private val followerState: Transition[Command] = eventCatamorphism(electionTimeout = becomeCandidate)

  private val candidateState: Transition[Command] =
    eventCatamorphism(electionTimeout = becomeCandidate, majorityVoteReceived = becomeLeader, leaderDiscovered = becomeFollower)

  private val leaderState: Transition[Command] = eventCatamorphism(nodeWithHigherTermDiscovered = becomeFollower)

  private val table = Map[StateCode, Transition[Command]](
    Follower -> followerState,
    Candidate -> candidateState,
    Leader -> leaderState
  )

  private def eventCatamorphism(
      electionTimeout: Transition[Command] = identity,
      majorityVoteReceived: Transition[Command] = identity,
      nodeWithHigherTermDiscovered: Transition[Command] = identity,
      leaderDiscovered: Transition[Command] = identity)(rc: RaftState[Command], e: NodeEvent): RaftState[Command] =
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

  def apply(rc: RaftState[Command], e: NodeEvent): RaftState[Command] =
    table(keyFn(rc)).apply(rc, e)
}
