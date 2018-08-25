package io.iohk.cef.consensus.raft

import akka.testkit.ImplicitSender
import io.iohk.cef.consensus.raft.model.{Entry}
import org.scalatest.BeforeAndAfterEach

import scala.collection._
import io.iohk.cef.consensus.raft.protocol._
class FollowerTest extends RaftSpec with BeforeAndAfterEach
  with ImplicitSender {

  behavior of "Follower"

  val initialMembers = 3

  it should "amortize taking the same write twice, the log should not contain duplicates then" in {
    // given

    subscribeBeginAsFollower()
    val m = awaitBeginAsFollower()
    val follower = m.ref
    val t = m.term
    info(s"$t")

    val msg = AppendEntries(t, t, 1, immutable.Seq(Entry("a", t, 1)), 0,self)

    // when
    info("Sending Append(a)")
    follower ! msg

    info("Sending Append(a)")
    follower ! msg

    // then
    expectMsg(AppendSuccessful(t, 1))
    expectMsg(AppendSuccessful(t, 1))
  }


  it should "reply with Vote if Candidate has later Term than the follower" in {

    restartMember()
    // given
    subscribeBeginAsFollower()

    info("Waiting for the follower...")
    val msg = awaitBeginAsFollower()

    val follower = msg.ref
    val msgTerm = msg.term
    info(s"Member $follower become a follower in $msgTerm")

    val nextTerm = msgTerm.next
    info(s"Requesting vote from member in a higher term $nextTerm...")
    follower ! RequestVote(nextTerm, self, msgTerm, 2)

    fishForMessage() {
      case msg @ VoteCandidate(term) if term == nextTerm => true
      case m => false
    }
  }
//
  it should "Reject if Candidate has lower Term than follower" in {
    // given

    restartMember()

    subscribeBeginAsFollower()

    info("Waiting for the follower...")
    val msg = awaitBeginAsFollower()
    val follower = msg.ref
    val msgTerm = msg.term

    info(s"Member $follower become a follower in $msgTerm")
    val prevTerm = msgTerm.prev

    // when
    info(s"Requesting vote from member with a stale term $prevTerm...")
    follower ! RequestVote(prevTerm, self, prevTerm, 1)

    // then
    expectMsg(DeclineCandidate(msg.term))
  }

  it should "only vote once during a Term" in {
    // given
    restartMember()

    subscribeBeginAsFollower()

    info("Waiting for the follower...")
    val msg = awaitBeginAsFollower()
    val follower = msg.ref
    val msgTerm = msg.term

    info(s"Member $follower become a follower in $msgTerm")

    // when / then
    info(s"Requesting vote from member within current term $msgTerm for the first time")
    follower ! RequestVote(msg.term, self, msg.term, 2)
    expectMsg(VoteCandidate(msg.term))

    info(s"Requesting vote from member within current term $msgTerm for the second time")
    follower ! RequestVote(msg.term, self, msg.term, 2)
    expectMsg(DeclineCandidate(msg.term))
  }

}
