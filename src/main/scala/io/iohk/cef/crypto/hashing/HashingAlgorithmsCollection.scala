package io.iohk.cef.crypto
package hashing

import enumeratum._

class HashingAlgorithmsCollection {

  sealed abstract class HashingAlgorithmType(private[crypto] val algorithm: HashAlgorithm) extends EnumEntry {
    def algorithmIdentifier = entryName
  }

  object HashingAlgorithmType extends Enum[HashingAlgorithmType] {

    val values = findValues

    case object KECCAK256 extends HashingAlgorithmType(algorithms.KECCAK256)
  }

  def apply(identifier: String): Option[HashingAlgorithmType] =
    HashingAlgorithmType.withNameOption(identifier)
}

object HashingAlgorithmsCollection {
  def apply(): HashingAlgorithmsCollection = new HashingAlgorithmsCollection()
}
