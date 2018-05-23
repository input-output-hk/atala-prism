package io.iohk.cef

import java.net.{InetAddress, InetSocketAddress}
import java.security.SecureRandom
import java.time.Clock

import akka.actor.typed.scaladsl.adapter._
import akka.util.ByteString
import akka.{actor => untyped}
import io.iohk.cef.db.DummyKnownNodesStorage
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.NodeStatus.NodeState
import io.iohk.cef.network.{Capabilities, Node, NodeStatus, ServerStatus}
import io.iohk.cef.utils.Logger

import scala.concurrent.duration._

trait AppBase extends Logger {

    implicit lazy val actorSystem = untyped.ActorSystem("cef_system")

    val address: Array[Byte] = Array(127.toByte,0,0,1)
    val localhost = InetAddress.getByAddress("",address)

    val discoveryConfig = new DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = 8090,
      bootstrapNodes = Set(Node(ByteString("1"),new InetSocketAddress(localhost, 8091), new InetSocketAddress(localhost, 8091), Capabilities(0x0))),
      discoveredNodesLimit = 10,
      scanNodesLimit = 10,
      concurrencyDegree = 20,
      scanInitialDelay = 10.seconds,
      scanInterval = 100.seconds,
      messageExpiration = 100.minute,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true)

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryWireMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryWireMessage]]

    def createActor(id: Int, bootstrapNodeIds: Set[Int], capabilities: Capabilities) = {
      val state = new NodeState(ByteString(id), ServerStatus.NotListening, ServerStatus.NotListening, capabilities)
      val portBase = 8090
      val bNodes = bootstrapNodeIds.map(nodeId =>
        Node(ByteString(nodeId.toString), new InetSocketAddress(localhost, portBase + nodeId), new InetSocketAddress(localhost, portBase + nodeId), state.capabilities)
      )
      val config = discoveryConfig.copy(port = portBase + id, bootstrapNodes = bNodes)
      val system = untyped.ActorSystem("cef_system" + id)
      val stateHolder = system.spawn(NodeStatus.nodeState(state, Seq()), "stateHolder")
      val secureRandom = new SecureRandom()
      val actor = system.actorOf(DiscoveryManager.props(
        config,
        new DummyKnownNodesStorage(Clock.systemUTC()),
        //new ScalikeKnownNodeStorage(Clock.systemUTC()),
        stateHolder,
        Clock.systemUTC(),
        encoder,
        decoder,
        DiscoveryManager.listenerMaker(config, stateHolder, encoder, decoder),
        system.scheduler,
        secureRandom
      ))
      (system, actor)
    }
}
