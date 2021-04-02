package io.iohk.atala.prism.crypto

/**
  * HMAC-SHA-256 JavaScript implementation.
  *
  * @todo The implementation is mising due to the fact that, currently we are moving
  *       most of the JS reated crypto code to the Kotlin SDK.
  */
private[crypto] object HmacSha256Impl {

  /**
    * Compute HMAC-SHA-256 data authentication code using shared key.
    */
  def compute(key: Array[Byte], data: Array[Byte]): Array[Byte] = ???
}
