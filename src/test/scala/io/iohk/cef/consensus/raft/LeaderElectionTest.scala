package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.protocol._
import org.scalatest.concurrent.Eventually
class LeaderElectionTest extends RaftSpec with Eventually {

  behavior of "Raft Leader Election"

  val initialMembers = 5

  it should "elect initial Leader" in {
    // given
    subscribeBeginAsLeader()

    info("Before election: ")
    infoMemberStates()

    // when
    awaitBeginAsLeader()

    info("After election: ")
    infoMemberStates()

    // then
    eventually {
      leaders should have length 1
      candidates should have length 0
      followers should have length 4
    }
  }

  it should "elect replacement Leader if current Leader dies" in {
    // given
    subscribeBeginAsLeader()

    infoMemberStates()

    // when
    killLeader()

    // then
    awaitBeginAsLeader()
    info("New leader elected: ")
    infoMemberStates()

    eventually {
      leaders should have length 1
      candidates should have length 0
      followers should have length 3
    }
  }

   /**
    * //RAFT Paper Page 4 Leader Section
    * Upon election: send initial empty AppendEntries RPCs (heartbeat) to each server; repeat during idle periods to prevent election timeouts
    **
    */
  it should "Upon election: send initial empty AppendEntries RPCs (heartbeat)" in {
    // given Leader Elected
    subscribeHeartBeatAppendEntries()

    info("New leader elected Heart Beat Started: ")

    infoMemberStates()

    //then

    awaitExpectedHeartBeatAppendEntries()

  }


  override def beforeAll(): Unit =
    subscribeClusterStateTransitions()
  super.beforeAll()
}
