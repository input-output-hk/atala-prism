package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.node.{RaftNode, _}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.Matchers.{fail, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{ExecutionContext, Future}

trait FakeRaftNode[Command] {

  implicit val ec: ExecutionContext = ExecutionContext.global
  val timeout = Span(5, Seconds)
  val span = Span(50, Millis)
  implicit val futuresPatienceConfig: ScalaFutures.PatienceConfig =
    ScalaFutures.PatienceConfig(timeout = timeout, interval = span)

  val stateMachine: Command => Unit = mock[Command => Unit]
  val peer: RaftNode[Command] = mock[RaftNode[Command]]
  val rpc2: RPC[Command] = mock[RPC[Command]]
  val rpc3: RPC[Command] = mock[RPC[Command]]
  var appendCallback: EntriesToAppend[Command] => AppendEntriesResult = _
  var voteCallback: VoteRequested => RequestVoteResult = _
  val rpcFactory: RPCFactory[Command] = (node, appendEntriesCallback, requestVoteCallback, _) => {
    appendCallback = appendEntriesCallback
    voteCallback = requestVoteCallback
    if (node == "i2")
      rpc2
    else if (node == "i3")
      rpc3
    else
      fail("Test setup is wrong.")
  }

  def after[T](millis: Long)(t: => T): Future[T] = {
    Future {
      Thread.sleep(millis)
    }.map(_ => t)
  }

  def aRaftNode(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {
    import scala.concurrent.duration._
    val neverTimeout = (100 days, 200 days)
    new RaftNode[Command](
      "i1",
      Seq("i1", "i2", "i3"),
      rpcFactory,
      neverTimeout,
      neverTimeout,
      stateMachine,
      persistentStorage)
  }

  def aFollower(persistentStorage: PersistentStorage[Command]): RaftNode[Command] =
    aRaftNode(persistentStorage)

  def aCandidate(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {

    val raftNode = aRaftNode(persistentStorage)

    val (term, _) = persistentStorage.state

    when(rpc2.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = false)))
    when(rpc3.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = false)))

    raftNode.electionTimeout().futureValue

    raftNode.getRole shouldBe Candidate

    raftNode
  }

  def aLeader(persistentStorage: PersistentStorage[Command]): RaftNode[Command] = {
    val raftNode = aRaftNode(persistentStorage)

    val (term, _) = persistentStorage.state

    when(rpc2.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = true)))
    when(rpc3.requestVote(any[VoteRequested]))
      .thenReturn(Future(RequestVoteResult(term = term + 1, voteGranted = true)))

    when(rpc2.appendEntries(any[EntriesToAppend[Command]]))
      .thenReturn(Future(AppendEntriesResult(term + 1, success = true)))
    when(rpc3.appendEntries(any[EntriesToAppend[Command]]))
      .thenReturn(Future(AppendEntriesResult(term + 1, success = true)))

    raftNode.electionTimeout().futureValue

    raftNode.getRole shouldBe Leader

    raftNode
  }
}
