package io.iohk.node

import java.security.PublicKey

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._

package object models {

  sealed trait KeyUsage extends EnumEntry with UpperSnakecase

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage
    case object IssuingKey extends KeyUsage
    case object CommunicationKey extends KeyUsage
    case object AuthenticationKey extends KeyUsage

  }

  class DIDSuffix private (val suffix: String) extends AnyVal

  object DIDSuffix {
    def apply(didSuffix: String): DIDSuffix = {
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(didSuffix).matches())

      new DIDSuffix(didSuffix)
    }

    val DID_SUFFIX_RE = "^[0-9a-f]{64}$".r
  }

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: PublicKey)

  case class DIDData(didSuffix: DIDSuffix, keys: List[DIDPublicKey])
}
