package io.iohk.cef.ledger.identity

import java.time.Instant

import io.iohk.cef.ledger.BlockHeader
import io.iohk.cef.codecs.nio._

case class IdentityBlockHeader(created: Instant) extends BlockHeader
object IdentityBlockHeader {
  implicit val IdentityBlockHeaderEncDec: NioEncDec[IdentityBlockHeader] = {
    import io.iohk.cef.codecs.nio.auto._
    val e: NioEncoder[IdentityBlockHeader] = genericEncoder
    val d: NioDecoder[IdentityBlockHeader] = genericDecoder
    NioEncDec(e, d)
  }
}
