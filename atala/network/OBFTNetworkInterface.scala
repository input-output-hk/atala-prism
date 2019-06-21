package atala.network

import atala.obft.NetworkMessage
import io.iohk.decco.Codec
import monix.eval.Task
import monix.reactive.{Observable, Observer}

/** This class defines the interface for network interfaces to be used with [[atala.obft.OuroborosBFT]] instances.
  *
  * @param codec$Tx a codec for the type Tx
  * @tparam Server the type of server's identifiers.
  * @tparam Tx the type of the underlying transactions.
  */
abstract class OBFTNetworkInterface[Server, Tx: Codec] {
  import OBFTNetworkInterface.OBFTChannel

  /** This method initialises the interface.
    *
    * @return a task that could be used to track success of the operation.
    */
  def initialise(): Task[Unit]

  /** Map of pre-existing know peers. */
  def knownServers: Map[Int, Server]

  /** This method generates an [[OBFTNetworkInterface.OBFTChannel]] instance to administrate the communication
    * of [[atala.obft.OuroborosBFT]] instances.
    *
    * @return an instance of [[OBFTNetworkInterface.OBFTChannel]]
    */
  def networkChannel(): Task[OBFTChannel[Tx]]

  /** This method shuts down the interface and frees resources.
    *
    * @return a task that could be used to track success of the operation.
    */
  def shutdown(): Task[Unit]
}

object OBFTNetworkInterface {

  /**
    * The class implements a channel intended to be connected to [[atala.obft.OuroborosBFT]] instances.
    * In essence, it is a multicast channel that hides the addresses type.
    *
    * [[atala.obft.OuroborosBFT]] instances use two main streams to represent their communication with
    * other peers. An input stream that provides messages sent by other servers (provided by the
    * "in" method and, an output stream of messages that the instance wants to diffuse to other
    * servers provided by the "out" method.
    *
    * @tparam Tx the underlying transaction type.
    */
  abstract class OBFTChannel[Tx: Codec] {

    /** This method sends a message to the self [[atala.obft.OuroborosBFT]] instance. The message will be received in the
      * stream returned by the in method.
      *
      * @param m the message to transmit.
      */
    def feed(m: NetworkMessage[Tx]): Unit

    /** The stream of messages received by the self [[atala.obft.OuroborosBFT]] instance. */
    def in: Observable[NetworkMessage[Tx]]

    /** The stream of messages sent by the self [[atala.obft.OuroborosBFT]] instance. */
    def out: Observer[NetworkMessage.AddBlockchainSegment[Tx]]

    /** This method closes the channel and frees resources.*/
    def close(): Task[Unit]
  }
}
