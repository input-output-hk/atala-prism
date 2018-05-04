package io.iohk.cef

import java.net.InetAddress
import java.security.SecureRandom
import java.time.Clock

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.agent.Agent
import akka.util.ByteString
import io.iohk.cef.db.KnownNodesStorage
import io.iohk.cef.discovery._
import io.iohk.cef.encoding.{Decoder, Encoder}
import io.iohk.cef.network.{Capabilities, Node, NodeAddress, NodeStatus, ServerStatus}
import io.iohk.cef.utils.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class LogEverything extends Actor with ActorLogging {

  override def receive: Receive = {
    case m =>
      println(s"Message Received: $m")
  }
}

object LogEverything {
  def props() = Props(new LogEverything)
}

object App extends Logger {

  def main(args: Array[String]): Unit = {

    implicit lazy val actorSystem = ActorSystem("cef_system")

    val discoveryConfig = new DiscoveryConfig(
      discoveryEnabled = true,
      interface = "0.0.0.0",
      port = 8091,
      bootstrapNodes = Set(Node(ByteString("0"),NodeAddress(InetAddress.getLocalHost,0,8090), Capabilities(0x0))),
      nodesLimit = 10,
      scanMaxNodes = 10,
      scanInitialDelay = 10.seconds,
      scanInterval = 1.minute,
      messageExpiration = 100.minute,
      maxSeekResults = 10,
      multipleConnectionsPerAddress = true)

    val nodeStatus =
      NodeStatus(
        key = network.loadAsymmetricCipherKeyPair("/tmp/file", SecureRandom.getInstance("NativePRNGNonBlocking")),
        capabilities = Capabilities(0x01),
        serverStatus = ServerStatus.NotListening)

    import io.iohk.cef.encoding.rlp.RLPEncoders._
    import io.iohk.cef.encoding.rlp.RLPImplicits._

    val encoder = implicitly[Encoder[DiscoveryMessage, ByteString]]

    val decoder = implicitly[Decoder[ByteString, DiscoveryMessage]]

    val discoveryManager8091 =
      actorSystem.actorOf(DiscoveryManager.props(
        discoveryConfig,
        new KnownNodesStorage,
        Agent(nodeStatus),
        Clock.systemUTC(),
        encoder,
        decoder
      ))


    val discoveryManager8090 =
      actorSystem.actorOf(DiscoveryManager.props(
        discoveryConfig.copy(port = 8090, bootstrapNodes = Set(Node(ByteString("1"),NodeAddress(InetAddress.getLocalHost,0,8091), Capabilities(0x0)))),
        new KnownNodesStorage,
        Agent(nodeStatus),
        Clock.systemUTC(),
        encoder,
        decoder
      ))

    val spy = actorSystem.actorOf(LogEverything.props())

    actorSystem.eventStream.subscribe(spy, classOf[CompatibleNodeFound])
  }
}
