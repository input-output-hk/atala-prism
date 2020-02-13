package io.iohk.node

import java.security.{MessageDigest, PublicKey}
import java.time.LocalDate

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._

package object models {

  sealed trait KeyUsage extends EnumEntry with UpperSnakecase {
    def canIssue: Boolean = this == KeyUsage.IssuingKey
  }

  object KeyUsage extends Enum[KeyUsage] {
    val values = findValues

    case object MasterKey extends KeyUsage
    case object IssuingKey extends KeyUsage
    case object CommunicationKey extends KeyUsage
    case object AuthenticationKey extends KeyUsage

  }

  case class SHA256Digest(value: Array[Byte]) {
    require(value.length == SHA256Digest.BYTE_LENGTH)

    def hexValue: String = value.map("%02x".format(_)).mkString("")

    override def canEqual(that: Any): Boolean = that.isInstanceOf[SHA256Digest]

    override def equals(obj: Any): Boolean = {
      canEqual(obj) && (obj match {
        case SHA256Digest(otherValue) => value.sameElements(otherValue)
        case _ => false
      })
    }

    override def toString(): String = s"SHA256Digest($hexValue)"
  }

  object SHA256Digest {
    val BYTE_LENGTH = 32
    val HEX_STRING_RE = "^(?:[0-9a-fA-F]{2})+$".r

    private def messageDigest = MessageDigest.getInstance("SHA-256")
    def compute(data: Array[Byte]): SHA256Digest = {
      SHA256Digest(messageDigest.digest(data))
    }

    def fromHex(s: String): SHA256Digest = {
      assert(HEX_STRING_RE.pattern.matcher(s).matches())
      SHA256Digest(s.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray)
    }
  }

  class DIDSuffix private (val suffix: String) extends AnyVal {
    override def toString = suffix
  }

  object DIDSuffix {
    def apply(didSuffix: String): DIDSuffix = {
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(didSuffix).matches())

      new DIDSuffix(didSuffix)
    }

    def apply(digest: SHA256Digest): DIDSuffix = apply(digest.hexValue)

    val DID_SUFFIX_RE = "^[0-9a-f]{64}$".r
  }

  case class DIDPublicKey(didSuffix: DIDSuffix, keyId: String, keyUsage: KeyUsage, key: PublicKey)

  case class DIDData(didSuffix: DIDSuffix, keys: List[DIDPublicKey])

  class CredentialId private (val id: String) extends AnyVal

  object CredentialId {
    def apply(id: String): CredentialId = {
      require(DIDSuffix.DID_SUFFIX_RE.pattern.matcher(id).matches())

      new CredentialId(id)
    }

    def apply(digest: SHA256Digest): CredentialId = apply(digest.hexValue)

    val CREDENTIAL_ID_RE = "^[0-9a-f]{64}$".r
  }

  case class Credential(
      credentialId: CredentialId,
      issuer: DIDSuffix,
      contentHash: SHA256Digest,
      issuedOn: LocalDate,
      revokedOn: Option[LocalDate] = None,
      lastOperation: SHA256Digest
  )

  case class AtalaObject(
      objectId: SHA256Digest,
      sequenceNumber: Int,
      blockHash: Option[SHA256Digest],
      processed: Boolean
  )
}
