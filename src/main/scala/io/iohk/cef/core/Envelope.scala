package io.iohk.cef.core
import io.iohk.cef.LedgerId

/**
  *
  * @param transaction
  * @param ledgerId
  * @param destinationDescriptor a function from NodeInfo to Boolean expressing which nodes are intended to process this tx
  *                              It is the user's responsibility to validate that the destination nodes can handle the
  *                              ledger with id ledgerId
  * @tparam State the ledgerState
  */
case class Envelope[+D](content: D, ledgerId: LedgerId, destinationDescriptor: DestinationDescriptor)
