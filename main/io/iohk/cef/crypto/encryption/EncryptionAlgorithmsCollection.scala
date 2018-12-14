package io.iohk.cef.crypto
package encryption

import java.security.SecureRandom
import enumeratum._

class EncryptionAlgorithmsCollection(secureRandom: SecureRandom) {

  sealed abstract class EncryptionAlgorithmType(private[crypto] val algorithm: EncryptionAlgorithm) extends EnumEntry {
    def algorithmIdentifier = entryName
  }

  object EncryptionAlgorithmType extends Enum[EncryptionAlgorithmType] {

    val values = findValues

    case object RSA extends EncryptionAlgorithmType(new algorithms.RSA(secureRandom))
  }

  def apply(identifier: String): Option[EncryptionAlgorithmType] =
    EncryptionAlgorithmType.withNameOption(identifier)
}

object EncryptionAlgorithmsCollection {
  def apply(secureRandom: SecureRandom): EncryptionAlgorithmsCollection =
    new EncryptionAlgorithmsCollection(secureRandom)
}
