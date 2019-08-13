package io.iohk.crypto

/** Class representing a way to encode data together with signature
  *
  * When sending data sometimes we want to add some kind of signature to it. It can be done in many
  * ways, with the simplest being appending the signature to the message, and more complex way being
  * embedding the signature into the document itself.
  *
  * This interface is an attempt of unifying these possibilities. Signing is done in following way:
  * 1. Convert data into intermediate representation, that allows easy obtaining of bytes so sign
  *    and adding signature to such representation (e.g. byte sequence with placeholder)
  * 2. Use intermediate representation to obtain bytes representation and generate signature
  * 3. Add signature to the intermediate representation, obtaining encoded version
  *
  * Verifying is done in following way:
  * 1. Extract intermediate representation and signature from encoded version
  * 2. Obtain value from the intermediate representation if needed - might be required for signature
  *    metadata
  * 3. Use existing intermediate representation to obtain bytes representation and verify signature
  *
  * @see docs/signing.md
  * @tparam T type to be encoded
  */
trait SignableEncoding[T] {

  /** Intermediate type representing encoded data with information on how to add signature to it */
  type Enclosure

  /** Extracts bytes representation of object after enclosing
    *
    * @param enclosure intermediate representation of encoded data
    * @return byte representation of data that can be used for signing
    */
  def getBytesToSign(enclosure: Enclosure): Array[Byte]

  /** Adds signature to encoded version
    *
    * @param enclosure intermediate representation of encoded data
    * @param signature signature of data to combine with representation
    * @return encoded version of data with signature included
    */
  def compose(enclosure: Enclosure, signature: Array[Byte]): String

  /** Recovers signature and intermediate representation
    *
    * @param value encoded data with signature
    * @return pair of intermediate representation and signature
    */
  def decompose(value: String): (Enclosure, Array[Byte])

  /** Converts data to intermediate representation
    *
    * @param t data to be converted
    * @return intermediate representation
    */
  def enclose(t: T): Enclosure

  /** Obtains data value from intermediate representation
    *
    * @param enclosure intermediate representation
    * @return data value represented
    */
  def disclose(enclosure: Enclosure): T

  /** Encodes data value using provided signature generation function
    *
    * @param t value to be ecnoded
    * @param sign function taking byte representation of data as argument and returning signature
    * @return encoded data with signature included
    */
  def encodeAndSign(t: T)(sign: Array[Byte] => Array[Byte]): String = {
    val enclosure = enclose(t)
    val signature = sign(getBytesToSign(enclosure))
    compose(enclosure, signature)
  }

  /** Decodes data from representation, verifying included signature
    *
    * @param s encoded data
    * @param verify function (bytes: Array[Byte], signature: Array[Byte]) => Boolean that validates provided signature against byte representation
    * @return data value
    */
  def verifyAndDecode(s: String)(verify: (Array[Byte], Array[Byte]) => Boolean): Option[T] = {
    val (enclosure, signature) = decompose(s)

    if (verify(getBytesToSign(enclosure), signature)) {
      Some(disclose(enclosure))
    } else {
      None
    }

  }
}
