package io.iohk.cef.consensus.raft

import akka.testkit.ImplicitSender
import io.iohk.cef.consensus.raft.model.Entry
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import io.iohk.cef.consensus.raft.protocol._
class CandidateTest extends RaftSpec with BeforeAndAfterEach
  with Eventually
  with ImplicitSender with PersistenceCleanup {

  behavior of "Candidate"

  val initialMembers = 3


  it should "go back to Follower state if got message from elected Leader (from later Term)" in {
    // given
    subscribeBeginElection()

    implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(300, Millis)), interval = scaled(Span(50, Millis)))

    info("Waiting for election to start...")
    val msg = awaitElectionStarted()

    val candidate = msg.ref
    val term = msg.term
    val nextTerm = term.next
    info(s"Member $candidate become a Candidate in $term")

    val entry = Entry(AppendTestRPC("TEST"), nextTerm, 5)

    // when
    candidate ! AppendEntries(nextTerm, term, 6, entry :: Nil, 5,self)

    // then
    eventually {
      // should have reverted to Follower
      followers should contain(candidate)
    }
  }

  it should "reject candidate if got RequestVote message with a stale term number" in {
    restartMember(leaders.headOption)
    subscribeBeginElection()

    info("Waiting for election to start...")
    val msg = awaitElectionStarted()
    val candidate = msg.ref
    val term = msg.term
    val prevTerm = term.prev
    info(s"Member $candidate become a Candidate in $term")

    info(s"Requesting vote from member with a stale term $prevTerm...")
    candidate ! RequestVote(prevTerm, self, prevTerm, 1)

    fishForMessage() {
      case DeclineCandidate(msgTerm) if msgTerm == term => true
      case _ => false
    }
  }
//
//
  it should "reject candidate if got VoteCandidate message with a stale term number" in {
    restartMember(leaders.headOption)
    subscribeBeginElection()

    info("Waiting for election to start...")
    val msg = awaitElectionStarted()
    val candidate = msg.ref
    val term = msg.term
    val prevTerm = term.prev
    info(s"Member $candidate become a Candidate in $term")

    info(s"Voting for candidate from member with a stale term $prevTerm...")
    candidate ! VoteCandidate(prevTerm)

    fishForMessage() {
      case DeclineCandidate(msgTerm) if msgTerm == term => true
      case _ => false
    }
  }

  override def beforeAll(): Unit =
    subscribeClusterStateTransitions()
    super.beforeAll()
}
