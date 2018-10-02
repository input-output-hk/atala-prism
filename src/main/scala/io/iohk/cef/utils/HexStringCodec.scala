package io.iohk.cef.utils
import akka.util.ByteString
import org.bouncycastle.util.encoders.Hex

object HexStringCodec {

  def toHexString(bs: ByteString): String =
    Hex.toHexString(bs.toArray)

  def fromHexString(s: String): ByteString =
    ByteString(Hex.decode(s))
}
