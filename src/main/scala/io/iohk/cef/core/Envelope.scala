package io.iohk.cef.core
import io.iohk.cef.LedgerId
import io.iohk.cef.codecs.nio._
import scala.reflect.runtime.universe.TypeTag

/**
  *
  * @param transaction
  * @param ledgerId
  * @param destinationDescriptor a function from NodeInfo to Boolean expressing which nodes are intended to process this tx
  *                              It is the user's responsibility to validate that the destination nodes can handle the
  *                              ledger with id ledgerId
  * @tparam State the ledgerState
  */
case class Envelope[D](content: D, ledgerId: LedgerId, destinationDescriptor: DestinationDescriptor)
object Envelope {
  implicit def EnvelopeEncDec[D: NioEncDec]: NioEncDec[Envelope[D]] = {
    import io.iohk.cef.codecs.nio.auto._
    implicit val ttd: TypeTag[D] = NioEncDec[D].typeTag
    val e: NioEncoder[Envelope[D]] = genericEncoder
    val d: NioDecoder[Envelope[D]] = genericDecoder
    NioEncDec(e, d)
  }
}
