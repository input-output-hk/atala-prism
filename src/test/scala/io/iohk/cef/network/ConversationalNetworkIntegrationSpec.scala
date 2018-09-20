package io.iohk.cef.network
import io.iohk.cef.network.encoding.nio._
import org.mockito.Mockito.{atLeastOnce, verify}
import org.scalatest.FlatSpec
import org.scalatest.concurrent.Eventually._
import org.scalatest.mockito.MockitoSugar._

import scala.collection.mutable
import scala.concurrent.duration._

class ConversationalNetworkIntegrationSpec extends FlatSpec with NetworkFixture {

  implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 5 seconds)

  object Messages {
    case class A(i: Int, s: String)
    case class B(s: String, b: Boolean)
  }

  import Messages._

  behavior of "ConversationalNetwork"

  it should "send a message to a peer" in {

    val bobsStack = randomBaseNetwork(None)
    val bobsNodeId = bobsStack.transports.peerInfo.nodeId

    val bobsAInbox = mockHandler[A]
    val bobA = messageChannel(bobsStack, bobsAInbox)

    val bobsBInbox = mockHandler[B]
    val bobB = messageChannel(bobsStack, bobsBInbox)

    val alicesStack = randomBaseNetwork(Some(bobsStack))
    val alicesNodeId = alicesStack.transports.peerInfo.nodeId

    val alicesAInbox = mockHandler[A]
    val aliceA = messageChannel(alicesStack, alicesAInbox)

    val alicesBInbox = mockHandler[B]
    val aliceB = messageChannel(alicesStack, alicesBInbox)

    eventually {
      aliceA.sendMessage(bobsNodeId, A(1, "Hi Bob!"))
      verify(bobsAInbox, atLeastOnce()).apply(A(1, "Hi Bob!"))
    }
  }

  private def mockHandler[T]: T => Unit =
    mock[T => Unit]

  private class MessageLog[T]() {
    val messages = mutable.ListBuffer[(NodeId, T)]()

    val messageHandler: (NodeId, T) => Unit = (nodeId, message) => messages += nodeId -> message
  }

  // Create a typed message channel on top of a base network instance
  private def messageChannel[T: NioEncoder: NioDecoder](
      baseNetwork: BaseNetwork,
      messageHandler: T => Unit): ConversationalNetwork[T] = {
    val conversationalNetwork = new ConversationalNetwork[T](baseNetwork.networkDiscovery, baseNetwork.transports)
    conversationalNetwork.messageStream.foreach(msg => messageHandler(msg))
    conversationalNetwork
  }

}
