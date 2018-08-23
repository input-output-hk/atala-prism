package io.iohk.cef.consensus.raft
import io.iohk.cef.consensus.raft.RaftConsensus.ConsensusModule
import io.iohk.cef.consensus.raft.RaftConsensus.RaftRPC.RPCImpl
import org.scalatest.{FlatSpec, WordSpec}
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar._
import org.mockito.Mockito._

class RaftConsensusSpec extends WordSpec {

  type Command = String

  private val stateMachine: Command => Unit = mock[Command => Unit]

  private val rpcImpl = mock[RPCImpl[Command]]

  val consensus = new ConsensusModule[Command](stateMachine)

  "Rules for servers" when {
    "All servers" when {
      "commitIndex > lastApplied" should {
        "increment last applied" in {}
        "apply log[lastApplied] to the state machine" in {}
      }
    }
    "Followers" when {
      "receiving an appendEntries RPC" should {
        "respond" in {

        }
      }
      "receiving a requestVote RPC" should {
        "respond" in {

        }
      }
      "election timeout elapses without appendEntries" should {
        "become a candidate" in {

        }
      }
    }
  }

  it should "apply "
}
