package io.iohk.cef

import java.net.InetAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, NodeAddress, NodeStatus, ServerStatus}
import io.iohk.cef.utils.Logger

import scala.concurrent.duration._

class LogEverything extends untyped.Actor with untyped.ActorLogging {

  override def receive: Receive = {
    case m =>
      println(s"Message Received: $m")
  }
}

object LogEverything {
  def props() = untyped.Props(new LogEverything)
}

object App extends Logger {

  def main(args: Array[String]): Unit = {

    implicit lazy val actorSystem = untyped.ActorSystem("cef_system")

    val address: Array[Byte] = Array(127.toByte,0,0,1)
    val localhost = InetAddress.getByAddress("",address)

    val discoveryConfig = new DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = 8090,
      bootstrapNodes = Set(Node(ByteString("1"),NodeAddress(localhost,0,8091), Capabilities(0x0))),
      nodesLimit = 10,
      scanMaxNodes = 10,
      scanInitialDelay = 10.seconds,
      scanInterval = 10.seconds,
      messageExpiration = 100.minute,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true)

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryMessage]]

    def createActor(id: Int, bootstrapNodeIds: Set[Int], capabilities: Capabilities) = {
      val key = network.loadAsymmetricCipherKeyPair("/tmp/file", SecureRandom.getInstance("NativePRNGNonBlocking"))
      val state = NodeState(key, ServerStatus.NotListening, capabilities)
      val portBase = 8090
      val bootstrapNodes = bootstrapNodeIds.map(nodeId => Node(ByteString("0"),NodeAddress(localhost,0,portBase + nodeId), state.capabilities))
      val discoveryConfig0 = discoveryConfig.copy(port = portBase + id, bootstrapNodes = bootstrapNodes)
      val system = untyped.ActorSystem("cef_system" + id)
      val stateHolder = system.spawn(NodeStatus.nodeState(state, Seq()), "stateHolder")
      system.actorOf(DiscoveryManager.props(
        discoveryConfig,
        new KnownNodesStorage,
        stateHolder,
        Clock.systemUTC(),
        encoder,
        decoder,
        DiscoveryManager.listenerMaker(discoveryConfig0, stateHolder, encoder, decoder),
        system.scheduler
      ))
      system
    }

    createActor(0, Set(), Capabilities(1))

    createActor(1, Set(0), Capabilities(1))

    createActor(2, Set(1,2), Capabilities(1))

    createActor(3, Set(0,1), Capabilities(1))

    createActor(4, Set(2), Capabilities(1))

    val system = createActor(5, Set(4), Capabilities(1))

    val spy = system.actorOf(LogEverything.props())

    system.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
