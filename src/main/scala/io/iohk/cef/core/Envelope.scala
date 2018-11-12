package io.iohk.cef.core
import io.iohk.cef.ContainerId

/**
  *
  * @param transaction
  * @param containerId
  * @param destinationDescriptor a function from NodeInfo to Boolean expressing which nodes are intended to process this tx
  *                              It is the user's responsibility to validate that the destination nodes can handle the
  *                              ledger with id ledgerId
  * @tparam State the ledgerState
  */
case class Envelope[+D](content: D, containerId: ContainerId, destinationDescriptor: DestinationDescriptor) {

  def map[T](f: D => T): Envelope[T] = {
    Envelope(f(content), containerId, destinationDescriptor)
  }
}
