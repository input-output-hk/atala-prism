package io.iohk.cef.core.raftrpc

import io.iohk.cef.consensus.raft._
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.network.transport.tcp.NetUtils.{NetworkFixture, nodesArePeers}
import org.mockito.Mockito.when
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.Matchers._
import io.iohk.cef.network.encoding.nio._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class RaftRPCSpec extends FlatSpec {

  private implicit val patienceConfig = ScalaFutures.PatienceConfig(timeout = 1 second)

  behavior of "RaftRPC"

  it should "support appendEntries" in rpcFixture { fixture =>
    val entriesToAppend = EntriesToAppend(1, "leader", 1, 1, Seq(LogEntry("A", 1, 1)), 1)
    val appendResult = AppendEntriesResult(1, success = true)

    when(fixture.node2AppendEntriesCalled(entriesToAppend)).thenReturn(appendResult)

    val result = fixture.node1RPC.appendEntries(entriesToAppend).futureValue

    result shouldBe appendResult
  }

  it should "support requestVote" in rpcFixture { fixture =>
    val voteRequested = VoteRequested(1, "candidate", 1, 1)
    val voteResult = RequestVoteResult(1, voteGranted = true)

    when(fixture.node2RequestVoteCalled(voteRequested)).thenReturn(voteResult)

    val result = fixture.node1RPC.requestVote(voteRequested).futureValue

    result shouldBe voteResult
  }

  it should "support clientAppendEntries" in rpcFixture { fixture =>
    val clientEntries = Seq("A", "B", "C")
    val appendResult = Future(Right(()))

    when(fixture.node2ClientAppendCalled(clientEntries)).thenReturn(appendResult)

    val result = fixture.node1RPC.clientAppendEntries(clientEntries).futureValue

    result shouldBe Right(())
  }

  private def appendEntriesCalled = mock[EntriesToAppend[String] => AppendEntriesResult]
  private def requestVoteCalled = mock[VoteRequested => RequestVoteResult]
  private def clientAppendCalled = mock[Seq[String] => Future[Either[Redirect[String], Unit]]]

  private def rpc(
      homeNode: NetworkFixture,
      remoteNode: NetworkFixture,
      appendEntriesCalled: EntriesToAppend[String] => AppendEntriesResult,
      requestVoteCalled: VoteRequested => RequestVoteResult,
      clientAppendCalled: Seq[String] => Future[Either[Redirect[String], Unit]]): RaftRPC[String] = {
    val rpcFactory = new RaftRPCFactory[String](homeNode.networkDiscovery, homeNode.transports)
    rpcFactory.apply(remoteNode.nodeId.toString, appendEntriesCalled, requestVoteCalled, clientAppendCalled)
  }

  case class RPCFixture(
      node1Fixture: NetworkFixture,
      node2Fixture: NetworkFixture,
      node1RPC: RaftRPC[String],
      node2RPC: RaftRPC[String],
      node2AppendEntriesCalled: EntriesToAppend[String] => AppendEntriesResult,
      node2RequestVoteCalled: VoteRequested => RequestVoteResult,
      node2ClientAppendCalled: Seq[String] => Future[Either[Redirect[String], Unit]])

  def createRPCFixture(): RPCFixture = {
    val node1 = NetUtils.randomNetworkFixture()
    val node2 = NetUtils.randomNetworkFixture()

    nodesArePeers(node1, node2)

    val node2AppendEntriesCalled = appendEntriesCalled
    val node2RequestVoteCalled = requestVoteCalled
    val node2ClientAppendCalled = clientAppendCalled

    val node1RPC = rpc(node1, node2, appendEntriesCalled, requestVoteCalled, clientAppendCalled)

    val node2RPC = rpc(node2, node1, node2AppendEntriesCalled, node2RequestVoteCalled, node2ClientAppendCalled)

    RPCFixture(
      node1,
      node2,
      node1RPC,
      node2RPC,
      node2AppendEntriesCalled,
      node2RequestVoteCalled,
      node2ClientAppendCalled)
  }

  def rpcFixture(testCode: RPCFixture => Any): Unit = {
    val fixture = createRPCFixture()
    try {
      testCode(fixture)
    } finally {
      fixture.node1Fixture.transports.shutdown()
      fixture.node2Fixture.transports.shutdown()
    }
  }
}
