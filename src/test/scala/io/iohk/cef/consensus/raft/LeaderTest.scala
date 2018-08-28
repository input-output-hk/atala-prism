package io.iohk.cef.consensus.raft

import akka.testkit.ImplicitSender
import io.iohk.cef.consensus.raft.model.{Entry, Term}
import org.scalatest._
import io.iohk.cef.consensus.raft.protocol._


class LeaderTest extends RaftSpec with FlatSpecLike with Matchers
  with ImplicitSender
  with BeforeAndAfter with BeforeAndAfterAll {

  behavior of "Leader"

  override def initialMembers: Int = 3



  it should "StepDown as Leader  if got Append entry with higher term from some other leader" in {
    subscribeBeginElection()
    subscribeForAppendEntries()

    val candidate = awaitElectionStarted()
    val candidateActor =  candidate.ref
    candidateActor ! BeginElection

    val term = candidate.term
    val nextTerm = term.next
    info(s"Member $candidate become a Candidate in $term")

    val entry = Entry(AppendTestRPC("TEST"), nextTerm, 5)
    Thread.sleep(100)
    // when

    candidateActor ! AppendEntries(Term(10), term, 6, entry :: Nil, 5,self)

    probe.fishForMessage() {
      case AppendEntries(Term(10),_,_,_,_,_)  => true
      case msg @ _=> false
    }

  }

  it should " get rejected Append RPC if there is inconsistency in log index with the follower" in {
    subscribeBeginElection()
    subscribeForAppendEntries()

    val candidate = awaitElectionStarted()
    val candidateActor =  candidate.ref
    candidateActor ! BeginElection

    val term = candidate.term
    val nextTerm = term.next
    info(s"Member $candidate become a Candidate in $term")

    val entry = Entry(AppendTestRPC("TEST"), nextTerm, 5)
    Thread.sleep(100)
    // when

    candidateActor ! AppendEntries(Term(2), term, 6, entry :: Nil, 5,self)

    probe.fishForMessage() {
      case AppendEntries(Term(10),_,_,_,_,_)  => true
      case msg @ _=> false
    }

  }

  it should "commit an entry once it has been written by the majority of the Followers" in {
    subscribeBeginAsLeader()
    val msg = awaitBeginAsLeader()
    val leader = msg.ref

    leader ! ClientMessage(self, AppendTestRPC("a"))
    leader ! ClientMessage(self, AppendTestRPC("b"))
    leader ! ClientMessage(self, AppendTestRPC("c"))

    subscribeEntryComitted()
    awaitEntryComitted(1)
    awaitEntryComitted(2)
    awaitEntryComitted(3)
  }









  it should "reply with it's current configuration when asked to" in {
    // note: this is used when an actor has died and starts again in Init state

    val leader = leaders.head

    // when
    leader ! RequestConfiguration

    // then
    val initialMembers = members.toSet
    fishForMessage() {
      case ChangeConfiguration(StableClusterConfiguration(potentialMembers)) if initialMembers == potentialMembers  => true
      case msg @ _=> false
    }
  }

  override def beforeAll(): Unit =
    subscribeClusterStateTransitions()
    super.beforeAll()



}