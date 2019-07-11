package atala.network

import java.net.InetSocketAddress

import atala.helpers.monixhelpers._
import atala.logging.{AtalaLogging, Loggable}
import atala.network.OBFTPeerGroupNetworkInterface.OBFTPeerGroupChannel
import atala.obft.NetworkMessage
import atala.config.ServerAddress
import io.iohk.scalanet.peergroup.{Channel, InetMultiAddress, PeerGroup, UDPPeerGroup}
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{MulticastStrategy, Observable, Observer}
import monix.reactive.subjects.ConcurrentSubject
import io.iohk.decco.Codec

import scala.collection.mutable
import scala.concurrent.Future

class OBFTPeerGroupNetworkInterface[Address: Loggable, Tx: Codec](
    serverNumber: Int,
    val knownServers: Map[Int, Address],
    underlyingPeerGroup: PeerGroup[Address, Either[Unit, NetworkMessage[Tx]]]
)(implicit s: Scheduler)
    extends OBFTNetworkInterface[Address, Tx]
    with AtalaLogging {

  import OBFTNetworkInterface.OBFTChannel
  import OBFTPeerGroupNetworkInterface.LoggingFormat._

  override def initialise(): Task[Unit] = {
    logger.info(
      "Initializing network interface",
      "server" -> serverNumber,
      "own address" -> underlyingPeerGroup.processAddress,
      "known servers" -> knownServers
    )
    underlyingPeerGroup.initialize()
  }

  override def networkChannel(): Task[OBFTChannel[Tx]] = {

    // we will receive incoming connections from servers that have lower number than us
    val futureConnections: Observable[Channel[Address, Either[Unit, NetworkMessage[Tx]]]] = underlyingPeerGroup.server()
    // we only start connections with servers that have a lower number than us
    val initialConnections: List[Address] = knownServers.filterKeys(_ < serverNumber).values.toList
    Task
      .gatherUnordered(initialConnections map underlyingPeerGroup.client)
      .map(new OBFTPeerGroupChannel[Address, Tx](serverNumber, _, futureConnections))
  }

  override def shutdown(): Task[Unit] = {
    logger.info("Finalizing network interface", "server" -> serverNumber, "known servers" -> knownServers)
    underlyingPeerGroup.shutdown()
  }
}

object OBFTPeerGroupNetworkInterface {

  import OBFTNetworkInterface._
  import OBFTPeerGroupNetworkInterface.LoggingFormat._

  class OBFTPeerGroupChannel[Address: Loggable, Tx: Codec](
      serverNumber: Int,
      createdConnections: List[Channel[Address, Either[Unit, NetworkMessage[Tx]]]],
      incomingConnections: Observable[Channel[Address, Either[Unit, NetworkMessage[Tx]]]]
  )(implicit s: Scheduler)
      extends OBFTChannel[Tx]
      with AtalaLogging {

    logger.info("Creating network channel", "connections to be started by the server" -> createdConnections.map(_.to))

    createdConnections.foreach { ch =>
      logger.info("Initiating connection", "server" -> serverNumber, "to" -> ch.to)
      ch.sendMessage(Left(())).runAsync
    }

    private val obftOutputStream = ConcurrentSubject[NetworkMessage.AddBlockchainSegment[Tx]](MulticastStrategy.replay)

    (obftOutputStream oneach {
      broadcast
    }).subscribe()

    // This is the input stream that instances of OBFT will use to receive messages
    private val inputStream: ConcurrentSubject[NetworkMessage[Tx], NetworkMessage[Tx]] =
      ConcurrentSubject[NetworkMessage[Tx]](MulticastStrategy.replay)

    // We take all the messages that come from the connections we started and forward them to the input stream
    private val inputFromInitialConnections: Observable[NetworkMessage[Tx]] = Observable
      .fromIterable(createdConnections map {
        _.in.collect { case Right(m) => m }
      })
      .merge

    (inputFromInitialConnections oneach { newMessage =>
      inputStream.feedItem(newMessage)
    }).subscribe()

    val outputForReceivedConnections: mutable.Set[Channel[Address, Either[Unit, NetworkMessage[Tx]]]] = {
      import scala.collection.JavaConverters._
      java.util.Collections
        .newSetFromMap(
          new java.util.concurrent.ConcurrentHashMap[
            Channel[Address, Either[Unit, NetworkMessage[Tx]]],
            java.lang.Boolean
          ]
        )
        .asScala
    }

    (incomingConnections oneach { newPeer =>
      val in = newPeer.in
      logger.debug(s"Connection request received", "server" -> serverNumber, "from" -> newPeer.to)
      outputForReceivedConnections += newPeer
      (in oneach {
        case Left(_) => // ignore initialization
        case Right(m) => inputStream.feedItem(m)
      }).subscribe()
      logger.debug(s"Connection request established", "server" -> serverNumber, "from" -> newPeer.to)
    }).subscribe()

    override def feed(m: NetworkMessage[Tx]): Unit = inputStream.feedItem(m)

    override def in: Observable[NetworkMessage[Tx]] = inputStream

    def out: Observer[NetworkMessage.AddBlockchainSegment[Tx]] = obftOutputStream

    override def close(): Task[Unit] = {
      logger.info("Closing channel", "server" -> serverNumber)
      Task.gatherUnordered((createdConnections ++ outputForReceivedConnections).map(_.close())) map { _ =>
        ()
      }
    }

    private def broadcast(message: NetworkMessage.AddBlockchainSegment[Tx]): Future[Unit] = {
      logger.debug(
        "Sending messages",
        "server" -> serverNumber,
        "to" -> (createdConnections ++ outputForReceivedConnections).map(_.to)
      )
      foreach(_.sendMessage(Right(message)))
    }
    private def foreach[C](f: Channel[Address, Either[Unit, NetworkMessage[Tx]]] => Task[C]): Future[Unit] = {
      Task.gatherUnordered((createdConnections ++ outputForReceivedConnections) map f).runAsync.map { _ =>
        ()
      }
    }
  }

  def apply[Address: Loggable, Tx: Codec](
      serverNumber: Int,
      knownServers: Map[Int, Address],
      underlyingPeerGroup: PeerGroup[Address, Either[Unit, NetworkMessage[Tx]]]
  )(implicit s: Scheduler): OBFTPeerGroupNetworkInterface[Address, Tx] =
    new OBFTPeerGroupNetworkInterface[Address, Tx](serverNumber, knownServers, underlyingPeerGroup)

  def createUPDNetworkInterface[Tx: Codec](
      localNodeIndex: Int,
      localNodeAddress: ServerAddress,
      remoteNodes: Set[(Int, ServerAddress)]
  )(implicit s: Scheduler): OBFTNetworkInterface[InetMultiAddress, Tx] = {
    import io.iohk.decco.auto._

    implicit val inetMultiAddressLoggable: Loggable[InetMultiAddress] = Loggable.gen[InetMultiAddress](_.toString)

    OBFTPeerGroupNetworkInterface[InetMultiAddress, Tx](
      localNodeIndex,
      (remoteNodes map {
        case (i, ServerAddress(h, p)) =>
          (i, InetMultiAddress(new InetSocketAddress(h, p)))
      }).toMap,
      new UDPPeerGroup[Either[Unit, NetworkMessage[Tx]]](
        UDPPeerGroup.Config(new InetSocketAddress(localNodeAddress.host, localNodeAddress.port))
      )
    )
  }

  object LoggingFormat {
    implicit def mapLoggable[A: Loggable, B: Loggable]: Loggable[Map[A, B]] = Loggable.gen[Map[A, B]] { originalMap =>
      val formatedMap = originalMap map { case (a, b) => (Loggable[A].log(a), Loggable[B].log(b)) }
      formatedMap.mkString("{", ", ", "}")
    }
    implicit def listLoggable[A: Loggable]: Loggable[List[A]] = Loggable.gen[List[A]] { originalList =>
      val formatedList = originalList map { Loggable[A].log }
      formatedList.mkString("[", ", ", "]")
    }

  }

}
