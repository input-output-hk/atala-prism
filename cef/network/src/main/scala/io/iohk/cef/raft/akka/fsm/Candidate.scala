package io.iohk.cef.raft.akka.fsm
import protocol._
trait Candidate {
  this : RaftActor =>

  val candidateBehavior: StateFunction = {

    // election
    case Event(msg @ BeginElectionEvent, sd: StateData) =>

      if (sd.config.members.isEmpty) {
        log.warning("Tried to initialize election with no members...")
        goto(Follower) applying GoToFollowerEvent()
      } else {
        log.info("Initializing election (among {} nodes) for {}", sd.config.members.size, sd.currentTerm)
        val request = RequestVote(sd.currentTerm, sd.self, logEntries.lastTerm, logEntries.lastIndex)
        sd.membersExceptSelf foreach {
          _ ! request
        }
        stay() applying VoteForSelfEvent()
      }

    case Event(msg: RequestVote, sd: StateData) if msg.term < sd.currentTerm =>
      log.info("Rejecting RequestVote msg by {} in {}. Received stale {}.", candidate, sd.currentTerm, msg.term)
      candidate ! DeclineCandidateEvent(sd.currentTerm)
      stay()

    case Event(msg: RequestVote, sd: StateData) if msg.term > sd.currentTerm =>
      log.info("Received newer {}. Current term is {}. Revert to follower state.", msg.term, sd.currentTerm)
      goto(Follower) applying GoToFollowerEvent(Some(msg.term))

    case Event(msg: RequestVote, sd: StateData) =>
      if (sd.canVoteIn(msg.term)) {
        log.info("Voting for {} in {}.", candidate, sd.currentTerm)
        candidate ! VoteCandidateEvent(sd.currentTerm)
        stay() applying VoteForEvent(candidate)
      } else {
        log.info("Rejecting RequestVote msg by {} in {}. Already voted for {}", candidate, sd.currentTerm, sd.votedFor.get)
        sender ! DeclineCandidateEvent(sd.currentTerm)
        stay()
      }

    case Event(VoteCandidateEvent(term), sd: StateData) if term < sd.currentTerm =>
      log.info("Rejecting VoteCandidate msg by {} in {}. Received stale {}.", voter(), sd.currentTerm, term)
      voter ! DeclineCandidateEvent(sd.currentTerm)
      stay()

    case Event(VoteCandidateEvent(term), sd: StateData) if term > sd.currentTerm =>
      log.info("Received newer {}. Current term is {}. Revert to follower state.", term, sd.currentTerm)
      goto(Follower) applying GoToFollowerEvent(Some(term))

    case Event(VoteCandidateEvent(term), sd: StateData) =>
      val votesReceived = sd.votesReceived + 1

      val hasWonElection = votesReceived > sd.config.members.size / 2
      if (hasWonElection) {
        log.info("Received vote by {}. Won election with {} of {} votes", voter(), votesReceived, sd.config.members.size)
        goto(Leader) applying GoToLeaderEvent()
      } else {
        log.info("Received vote by {}. Have {} of {} votes", voter(), votesReceived, sd.config.members.size)
        stay applying IncrementVoteEvent()
      }

    case Event(DeclineCandidateEvent(term), sd: StateData) =>
      if (term > sd.currentTerm) {
        log.info("Received newer {}. Current term is {}. Revert to follower state.", term, sd.currentTerm)
        goto(Follower) applying GoToFollowerEvent(Some(term))
      } else {
        log.info("Candidate is declined by {} in term {}", sender(), sd.currentTerm)
        stay()
      }

    // end of election


    // ending election due to timeout
    case Event(ElectionTimeout, sd: StateData) if sd.config.members.size > 1 =>
      log.info("Voting timeout, starting a new election (among {})...", sd.config.members.size)
      sd.self ! BeginElectionEvent
      stay() applying StartElectionEvent()

    // would like to start election, but I'm all alone!
    case Event(ElectionTimeout, m: StateData) =>
      log.info("Voting timeout, unable to start election, don't know enough nodes (members: {})...", m.config.members.size)
      goto(Follower) applying GoToFollowerEvent()


  }

}
