package io.iohk.cef.consensus.raft

import akka.testkit.TestProbe
import org.scalatest.concurrent.Eventually
import io.iohk.cef.consensus.raft.protocol._
import scala.concurrent.duration._

class LogReplicationTest extends RaftSpec
  with Eventually with PersistenceCleanup {

  behavior of "Log Replication"

  val initialMembers = 5

  val timeout = 2.second

  val client = TestProbe()

  it should "apply the state machine in expected order" in {
    // given
    subscribeBeginAsLeader()
    val msg = awaitBeginAsLeader()
    val leader = msg.ref
    infoMemberStates()

    // when
    leader ! ClientMessage(client.ref, AppendTestRPC("My"))       // 0
    leader ! ClientMessage(client.ref, AppendTestRPC("State"))    // 1
    leader ! ClientMessage(client.ref, AppendTestRPC("Changed"))  // 2
    leader ! ClientMessage(client.ref, GetTestRPCs)               // 3

    // then
    client.expectMsg(timeout, "My")
    client.expectMsg(timeout, "State")
    client.expectMsg(timeout, "Changed")

    val stateMachineReceived = client.expectMsg(timeout, List("My", "State", "Changed"))
    info("Final replicated state machine state: " + stateMachineReceived)
  }


  override def beforeAll(): Unit =
    subscribeClusterStateTransitions()
    super.beforeAll()
}
