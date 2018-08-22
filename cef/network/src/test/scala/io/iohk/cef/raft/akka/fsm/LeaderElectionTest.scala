package io.iohk.cef.raft.akka.fsm

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}

class LeaderElectionTest extends RaftSpec with Eventually {

  behavior of "Raft Leader Election"

  override implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(2, Seconds)), interval = scaled(Span(100, Millis)))

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




  override def beforeAll(): Unit =
    subscribeClusterStateTransitions()
    super.beforeAll()
}
