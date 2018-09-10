package io.iohk.cef.core.raftrpc

import io.iohk.cef.consensus.raft._
import io.iohk.cef.network.transport.tcp.NetUtils
import io.iohk.cef.network.transport.tcp.NetUtils.{NetworkFixture, nodesArePeers}
import org.mockito.Mockito.when
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar._
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.Matchers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RaftRPCSpec extends FlatSpec {
  behavior of "RaftRPC"

  it should "support outbound appendEntries" in {
    val node1 = NetUtils.randomNetworkFixture()
    val node2 = NetUtils.randomNetworkFixture()

    nodesArePeers(node1, node2)

    val node1AppendEntriesCalled = appendEntriesCalled
    val node2AppendEntriesCalled = appendEntriesCalled

    val node1RPC = rpc(node1, node1AppendEntriesCalled, requestVoteCalled)
    val node2RPC = rpc(node2, node2AppendEntriesCalled, requestVoteCalled)

    val entriesToAppend = EntriesToAppend(1, "leader", 1, 1, Seq(LogEntry("A", 1, 1)), 1)
    val appendResult = AppendEntriesResult(1, success = true)

    when(node2RPC.appendEntries(entriesToAppend)).thenReturn(Future(appendResult))

    val result = node1RPC.appendEntries(entriesToAppend).futureValue

    result shouldBe appendResult
  }

  it should "support outbound requestVote" in {}

  it should "support outbound clientAppendEntries" in {}

  private def appendEntriesCalled = mock[EntriesToAppend[String] => AppendEntriesResult]
  private def requestVoteCalled = mock[VoteRequested => RequestVoteResult]

  private def rpc(
      node: NetworkFixture,
      appendEntriesCalled: EntriesToAppend[String] => AppendEntriesResult,
      requestVoteCalled: VoteRequested => RequestVoteResult): RaftRPC[String] = ???
//  {
//    val rpcFactory = new RaftRPCFactory[String](node.networkDiscovery, node.transports)
//    rpcFactory.apply(node.nodeId.toString, appendEntriesCalled, requestVoteCalled)
//  }
}
