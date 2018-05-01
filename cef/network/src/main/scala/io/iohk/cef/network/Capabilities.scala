package io.iohk.cef.network

//Byte is probably not the best implementation.
case class Capabilities(byte: Byte) {

  /**
    * Tells if this object satisfies the other object's capabilities. In other words, it has at least the other object's
    * capabilities
    * @param other
    * @return
    */
  def satisfies(other: Capabilities) = (byte & other.byte) == other.byte
}
