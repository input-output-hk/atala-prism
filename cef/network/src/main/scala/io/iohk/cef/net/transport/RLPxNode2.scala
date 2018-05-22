package io.iohk.cef.net.transport

import java.net.InetSocketAddress
import java.security.SecureRandom

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import io.iohk.cef.net.SimpleNode.StartServer
import io.iohk.cef.net.transport.rlpx.RLPxConnectionHandler.{ConnectionEstablished, RLPxConfiguration, SendMessage}
import io.iohk.cef.net.transport.rlpx.ethereum.p2p.Message.Version
import io.iohk.cef.net.transport.rlpx.ethereum.p2p.{Message, MessageDecoder, MessageSerializable}
import io.iohk.cef.net.transport.rlpx.{AuthHandshaker, ECPublicKeyParametersNodeId, RLPxConnectionHandler, loadAsymmetricCipherKeyPair}
import io.iohk.cef.net.{NodeInfo, SimpleNode}
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.util.encoders.Hex

import scala.concurrent.duration.{FiniteDuration, _}

object RLPxNode2 extends App {

  val config = ConfigFactory.parseString(
    """
      |akka {
      |  loglevel = "DEBUG"
      |  actor {
      |    debug {
      |      # enable function of LoggingReceive, which is to log any received message at
      |      # DEBUG level
      |      receive = on
      |    }
      |  }
      |}
    """.stripMargin)

  object UserCode {

    case class SampleMessage(content: String) extends MessageSerializable {
      override def toBytes(implicit di: DummyImplicit): ByteString = ByteString(content)

      override def toBytes: Array[Byte] = content.getBytes

      override def underlyingMsg: Message = this

      override def code: Version = 1
    }

    private val sampleMessageDecoder = new MessageDecoder {
      override def fromBytes(`type`: Int, payload: Array[Byte], protocolVersion: Version): Message = SampleMessage(new String(payload))
    }

    private val rlpxConfiguration = new RLPxConfiguration {
      override val waitForHandshakeTimeout: FiniteDuration = 3 seconds
      override val waitForTcpAckTimeout: FiniteDuration = 3 seconds
    }

    private val secureRandom: SecureRandom = new SecureRandom()

    // how mantis generates node ids.
    def nodeIdFromKey(nodeKey: AsymmetricCipherKeyPair): String = {
      Hex.toHexString(new ECPublicKeyParametersNodeId(nodeKey.getPublic.asInstanceOf[ECPublicKeyParameters]).toNodeId)
    }

    // mantis uses loadAsymmetricCipherKeyPair to lazily generate node keys
    def nodeKeyFromName(nodeName: String): AsymmetricCipherKeyPair = {
      loadAsymmetricCipherKeyPair(s"/tmp/${nodeName}_key", secureRandom)
    }

    def createNode(nodeName: String, context: ActorContext, bootstrapPeers: Option[NodeInfo] = None): (String, ActorRef) = {
      val nodeKey = nodeKeyFromName(nodeName)
      val nodeId = nodeIdFromKey(nodeKey)

      val authHandshaker = AuthHandshaker(nodeKey, secureRandom)

      val props = RLPxConnectionHandler.props(sampleMessageDecoder, protocolVersion = 1, authHandshaker, rlpxConfiguration)

      val rlpxHandler = context.actorOf(props, name = s"RLPxNode_$nodeName")

      (nodeId, context.actorOf(Props(new SimpleNode(nodeId, rlpxHandler, bootstrapPeers))))
    }

    def createBootstrapNode(context: ActorContext): NodeInfo = {
      val (bootstrapNodeId, bootstrapNode) = createNode("A", context)
      val bootstrapAddr = new InetSocketAddress("localhost", 3000)

//      bootstrapNode ! StartServer(bootstrapAddr)

      NodeInfo(bootstrapNodeId, bootstrapAddr)
    }

  }

  import UserCode._





  class ConversationActor extends Actor with ActorLogging {

    val bootstrapNodeInfo = createBootstrapNode(context)
    val (nextActorId, nextActor) = createNode("B", context, Some(bootstrapNodeInfo))

    override def receive: Receive = {
      case ConnectionEstablished(x) =>
        log.debug(s"Connection established to ${Hex.toHexString(x.toArray)}")
        sender() ! SendMessage(SampleMessage("Hello, peer!"))
      case _ => nextActor ! StartServer(new InetSocketAddress("localhost", 0))
    }
  }

  implicit val system: ActorSystem = ActorSystem("RLPxNode", config)
  val conversationActor = system.actorOf(Props[ConversationActor], "Conversation")
  conversationActor ! "go"
}
