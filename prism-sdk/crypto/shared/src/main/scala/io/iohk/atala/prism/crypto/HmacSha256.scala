package io.iohk.atala.prism.crypto

/**
  * HMAC-SHA-256 facade.
  */
object HmacSha256 {

  /**
    * Compute HMAC-SHA-256 data authentication code using shared key.
    */
  def compute(key: Array[Byte], data: Array[Byte]): Array[Byte] =
    HmacSha256Impl.compute(key, data)
}
