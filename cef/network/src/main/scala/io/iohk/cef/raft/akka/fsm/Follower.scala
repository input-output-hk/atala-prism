package io.iohk.cef.raft.akka.fsm
import io.iohk.cef.raft.akka.fsm.protocol._

trait Follower {
  this : RaftActor =>


  val followerEvents : StateFunction = {
    case Event(BeginAsFollower(term, _), myState: StateData) =>
      stay()

    // timeout,  Need to start an election
    case Event(StateTimeout, myState: StateData) =>
      beginElection(myState)

    // election
    case Event(r @ RequestVote(term, candidate, lastLogTerm, lastLogIndex), myState: StateData)
      if term > myState.currentTerm =>
      log.info("Received newer {}. Current term is {}.", term, myState.currentTerm)
      stay() applying UpdateTermEvent(term)

    case Event(RequestVote(term, candidate, lastLogTerm, lastLogIndex), myState: StateData)
        if myState.canVoteIn(term) =>
        // Check if the log is up-to-date before granting vote.
        // Raft determines which of two logs is more up-to-date
        // by comparing the index and term of the last entries in the
        // logs. If the logs have last entries with different terms, then
        // the log with the later term is more up-to-date. If the logs
        // end with the same term, then whichever log is longer is
        // more up-to-date.
        if (lastLogTerm < replicatedLog.lastTerm) {
          log.info("Rejecting vote for {}, and {}. Candidate's lastLogTerm: {} < ours: {}",
            candidate, term, lastLogTerm, replicatedLog.lastTerm)
          sender ! DeclineCandidate(myState.currentTerm)
          stay()
        } else if (lastLogTerm == replicatedLog.lastTerm &&
          lastLogIndex < replicatedLog.lastIndex) {
          log.info("Rejecting vote for {}, and {}. Candidate's lastLogIndex: {} < ours: {}",
            candidate, term, lastLogIndex, replicatedLog.lastIndex)
          sender ! DeclineCandidate(myState.currentTerm)
          stay()
        } else {
          log.info("Voting for {} in {}", candidate, term)
          sender ! VoteCandidate(myState.currentTerm)

          stay() applying VoteForEvent(candidate)
        }

    case Event(RequestVote(term, candidateId, lastLogTerm, lastLogIndex), myState: StateData) if myState.votedFor.isDefined =>
      log.info("Rejecting vote for {}, and {}, currentTerm: {}, already voted for: {}", candidate(), term, myState.currentTerm, myState.votedFor.get)
      sender ! DeclineCandidate(myState.currentTerm)
      stay()

    case Event(RequestVote(term, candidateId, lastLogTerm, lastLogIndex), myState: StateData) =>
      log.info("Rejecting vote for {}, and {}, currentTerm: {}, received stale term number {}", candidate(), term, myState.currentTerm, term)
      sender ! DeclineCandidate(myState.currentTerm)
      stay()

    // end of election

    // take writes
    case Event(msg: AppendEntries[_], sd: StateData) =>
      // First check the consistency of this request
      if (!replicatedLog.containsMatchingEntry(msg.prevLogTerm, msg.prevLogIndex)) {
        log.info("Rejecting write (inconsistent log): {} {} {} ", msg.prevLogIndex, msg.prevLogTerm, replicatedLog)
        leader ! AppendRejected(sd.currentTerm)
        stay()
      } else {
        //TODO
        appendEntries(msg, sd)
      }

    // end of take writes

  }


  def  followerStateHandler():Unit = {
    self ! BeginAsFollower(stateData.currentTerm, self)
  }



  def appendEntries(msg: AppendEntries[_], sd: StateData): State = {

    //TODO
    acceptHeartbeat()
  }

}
