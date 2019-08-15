package atala.network

import atala.obft.NetworkMessage
import monix.eval.Task
import monix.reactive.{Observable, Observer}
import io.iohk.decco.Codec
import atala.config.ServerAddress
import atala.logging.Loggable
import monix.execution.Scheduler

trait OBFTNetworkFactory[Tx] {

  /** This method initialises the network interface.
    *
    * @return a task that provides a network interface for OBFT.
    */
  def initialise(): Task[OBFTNetworkInterface[Tx]]

}

object OBFTNetworkFactory {

  def apply[Tx: Codec](
      localNodeIndex: Int,
      localNodeAddress: ServerAddress,
      remoteNodes: Set[(Int, ServerAddress)]
  )(implicit s: Scheduler): OBFTNetworkFactory[Tx] = {
    import io.iohk.decco.auto._
    import io.iohk.scalanet.peergroup.{InetMultiAddress, UDPPeerGroup}
    import java.net.InetSocketAddress

    implicit val inetMultiAddressLoggable: Loggable[InetMultiAddress] = Loggable.gen[InetMultiAddress](_.toString)
    val peerGroup =
      new UDPPeerGroup[NetworkMessage[Tx]](
        UDPPeerGroup.Config(new InetSocketAddress(localNodeAddress.host, localNodeAddress.port))
      )
    val knownServers =
      (remoteNodes map {
        case (i, ServerAddress(h, p)) =>
          (i, InetMultiAddress(new InetSocketAddress(h, p)))
      }).toMap

    new OBFTPeerGroupNetworkFactory[Tx, InetMultiAddress](
      localNodeIndex,
      knownServers,
      peerGroup
    )

  }
}

trait OBFTNetworkInterface[Tx] {

  /** The stream of messages received from the network. */
  def in: Observable[NetworkMessage[Tx]]

  /** The stream of messages sent to the network. */
  def out: Observer[NetworkMessage.AddBlockchainSegment[Tx]]

  def feed(msg: NetworkMessage[Tx]): Unit

  /** This method returns a task that shuts down the interface and frees resources.
    *
    * @return a task that when run, shuths down the interface and frees resources.
    */
  def shutdown(): Task[Unit]
}
