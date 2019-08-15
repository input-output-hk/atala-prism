package atala.network

import io.iohk.decco.Codec
import monix.reactive.{Observable, Observer}
import monix.eval.Task
import atala.obft.NetworkMessage
import atala.logging.{AtalaLogging, Loggable}
import io.iohk.decco.Codec
import io.iohk.scalanet.peergroup.{Channel, InetMultiAddress, PeerGroup, UDPPeerGroup}
import monix.reactive.{MulticastStrategy, Observable, Observer}
import monix.reactive.subjects.ConcurrentSubject
import monix.execution.Scheduler
import atala.helpers.monixhelpers._
import scala.concurrent.Future
import OBFTPeerGroupNetworkFactory._
import NetworkMessage.AddBlockchainSegment

object OBFTPeerGroupNetworkFactory {

  type Subject[T] = Observable[T] with Observer[T]
  def Subject[T](implicit s: Scheduler): Subject[T] =
    ConcurrentSubject[T](MulticastStrategy.replayLimited(512))

}

class OBFTPeerGroupNetworkFactory[Tx: Codec, Address: Loggable](
    serverNumber: Int,
    knownServers: Map[Int, Address],
    underlyingPeerGroup: PeerGroup[Address, NetworkMessage[Tx]]
)(implicit scheduler: Scheduler)
    extends OBFTNetworkFactory[Tx]
    with AtalaLogging {

  type PgChannel = Channel[Address, NetworkMessage[Tx]]

  override def initialise(): Task[OBFTPeerGroupNetworkInterface[Tx, Address]] = {
    val remoteAddresses: List[(Int, Address)] =
      knownServers.filterKeys(_ != serverNumber).toList

    for {
      _ <- underlyingPeerGroup.initialize()
      in <- genIn(underlyingPeerGroup.server())
      out <- genOut(remoteAddresses)
    } yield new OBFTPeerGroupNetworkInterface(in, out, underlyingPeerGroup)
  }

  private def genIn(
      createdConnections: Observable[PgChannel]
  ): Task[Subject[NetworkMessage[Tx]]] = {

    Task {
      val inputStream: Subject[NetworkMessage[Tx]] = Subject[NetworkMessage[Tx]]

      createdConnections.oneach { channel =>
        channel.in
          .oneach(inputStream.feedItem)
          .subscribe
      }.subscribe

      inputStream
    }

  }

  private def genOut(remoteAddresses: List[(Int, Address)]): Task[Observer[AddBlockchainSegment[Tx]]] = {
    val channelsTask: Task[List[(Int, PgChannel)]] =
      Task.gather(
        remoteAddresses
          .map {
            case (to, a) =>
              logger.trace("Initiating connection to remote node", "from" -> serverNumber, "to" -> to)
              underlyingPeerGroup.client(a).map { ch =>
                logger.trace("Connection to remote node stablished", "from" -> serverNumber, "to" -> to)
                (to, ch)
              }
          }
      )

    channelsTask.map { channels =>
      val obftOutputStream: Subject[AddBlockchainSegment[Tx]] =
        Subject[AddBlockchainSegment[Tx]]

      obftOutputStream
        .mapTask(
          m => Task.gather(channels.map { case (to, ch) => send(m, to, ch) })
        )
        .subscribe

      obftOutputStream
    }
  }

  private def send(m: AddBlockchainSegment[Tx], to: Int, ch: PgChannel): Task[Unit] = {
    logger.trace("Sending message", "from" -> serverNumber, "to" -> to)
    ch.sendMessage(m)
  }

}

class OBFTPeerGroupNetworkInterface[Tx: Codec, Address: Loggable](
    inputStream: Subject[NetworkMessage[Tx]],
    override val out: Observer[AddBlockchainSegment[Tx]],
    underlyingPeerGroup: PeerGroup[Address, NetworkMessage[Tx]]
) extends OBFTNetworkInterface[Tx] {

  override def in: Observable[NetworkMessage[Tx]] = inputStream
  def shutdown(): Task[Unit] = underlyingPeerGroup.shutdown
  def feed(msg: NetworkMessage[Tx]): Unit = {
    import monix.execution.Scheduler.Implicits.global
    inputStream.feedItem(msg)
  }
}
