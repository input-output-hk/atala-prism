package io.iohk.cef.network

import io.iohk.cef.encoding.rlp._

//How many ledgers do we need to support?
case class Capabilities(byte: Byte) {

  /**
    * Tells if this object satisfies the other object's capabilities. In other words, it has at least the other object's
    * capabilities
    * @param other
    * @return
    */
  def satisfies(other: Capabilities) = (byte & other.byte) == other.byte
}

object Capabilities {

  implicit val capabilitiesRLPEncDec = new RLPEncDec[Capabilities] {
    override def encode(obj: Capabilities): RLPEncodeable = {
      RLPValue(Array(obj.byte))
    }

    override def decode(rlp: RLPEncodeable): Capabilities = rlp match {
      case RLPValue(Array(byte)) => Capabilities(byte)
      case _ => throw new RLPException("src is not a Capabilities object")
    }
  }
}
