package io.iohk.cef.raft.akka.fsm
import protocol._

trait Candidate {
  this : RaftActor =>

  val candidateEvents : StateFunction = {

    // election
    case Event(BeginElection, sd: StateData) =>

      if (sd.config.members.isEmpty) {
        log.warning("Tried to initialize election with no members...")
        goto(Follower) applying GoToFollowerEvent()
      } else {
        log.info("Initializing election (among {} nodes) for {}", sd.config.members.size, sd.currentTerm)
        val request = RequestVote(sd.currentTerm, sd.self, replicatedLog.lastTerm, replicatedLog.lastIndex)
        sd.membersExceptSelf foreach {
          _ ! request
        }
        stay() applying VoteForSelfEvent()
      }
    // timeout,  Need to start an election
    case Event(StateTimeout, myState: StateData) => stay() applying StartElectionEvent()

    case Event(msg: RequestVote, sd: StateData) if msg.term < sd.currentTerm =>
      log.info("Rejecting RequestVote msg by {} in {}. Received stale {}.", candidate, sd.currentTerm, msg.term)
      candidate ! DeclineCandidate(sd.currentTerm)
      stay()

    case Event(msg: RequestVote, sd: StateData) if msg.term > sd.currentTerm =>
      log.info("Received newer {}. Current term is {}. Revert to follower state.", msg.term, sd.currentTerm)
      goto(Follower) applying GoToFollowerEvent(Some(msg.term))

    case Event(msg: RequestVote, sd: StateData) =>
      if (sd.canVoteIn(msg.term)) {
        log.info("Voting for {} in {}.", candidate, sd.currentTerm)
        candidate ! VoteCandidate(sd.currentTerm)
        stay() applying VoteForEvent(candidate)
      } else {
        log.info("Rejecting RequestVote msg by {} in {}. Already voted for {}", candidate, sd.currentTerm, sd.votedFor.get)
        sender ! DeclineCandidate(sd.currentTerm)
        stay()
      }

    case Event(VoteCandidate(term), sd: StateData) if term < sd.currentTerm =>
      log.info("Rejecting VoteCandidate msg by {} in {}. Received stale {}.", voter(), sd.currentTerm, term)
      voter ! DeclineCandidate(sd.currentTerm)
      stay()

    case Event(VoteCandidate(term), sd: StateData) if term > sd.currentTerm =>
      log.info("Received newer {}. Current term is {}. Revert to follower state.", term, sd.currentTerm)
      goto(Follower) applying GoToFollowerEvent(Some(term))

    case Event(VoteCandidate(term), sd: StateData) =>
      val votesReceived = sd.votesReceived + 1

      val hasWonElection = votesReceived > sd.config.members.size / 2
      if (hasWonElection) {
        log.info("Received vote by {}. Won election with {} of {} votes", voter(), votesReceived, sd.config.members.size)
        goto(Leader) applying GoToLeaderEvent()
      } else {
        log.info("Received vote by {}. Have {} of {} votes", voter(), votesReceived, sd.config.members.size)
        stay applying IncrementVoteEvent()
      }

    case Event(DeclineCandidate(term), sd: StateData) =>
      if (term > sd.currentTerm) {
        log.info("Received newer {}. Current term is {}. Revert to follower state.", term, sd.currentTerm)
        goto(Follower) applying GoToFollowerEvent(Some(term))
      } else {
        log.info("Candidate is declined by {} in term {}", sender(), sd.currentTerm)
        stay()
      }

    // end of election

    // handle appends
    case Event(append: AppendEntries[_], sd: StateData) =>
      val leaderIsAhead = append.term >= sd.currentTerm

      if (leaderIsAhead) {
        log.info("Reverting to Follower, because got AppendEntries from Leader in {}, but am in {}", append.term, sd.currentTerm)
        sd.self forward append
        goto(Follower) applying GoToFollowerEvent()
      } else {
        stay()
      }

  }

  def  candidateStateHandler():Unit = {
    self ! BeginElection
  }
}
