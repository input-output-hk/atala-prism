package io.iohk.cef.raft.akka.fsm.protocol

import akka.persistence.fsm.PersistentFSM.FSMState

trait RaftStates {

  sealed trait RaftState extends FSMState

  /** A Follower can take writes from a Leader; If doesn't get any heartbeat, may decide to become a Candidate */
  case object Follower extends RaftState {
    override def identifier: String = "Follower"
  }

  /** A Candidate tries to become a Leader, by issuing RequestVote */
  case object Candidate extends RaftState {
    override def identifier: String = "Candidate"
  }

  /** The Leader is responsible for taking writes, and committing entries, as well as keeping the heartbeat to all members */
  case object Leader extends RaftState {
    override def identifier: String = "Leader"
  }
}
