package io.iohk.atala.prism.identity

import io.iohk.atala.prism.crypto.Sha256Digest

import scala.util.Try

final class DIDSuffix private (val value: String) extends AnyVal {
  override def toString: String = value
}

object DIDSuffix {
  def fromString(suffix: String): Option[DIDSuffix] = {
    Try(DIDSuffix(suffix)).toOption
  }

  def unsafeFromDigest(digest: Sha256Digest): DIDSuffix = apply(digest.hexValue)

  def unsafeFromString(didSuffix: String): DIDSuffix = apply(didSuffix)

  private def apply(didSuffix: String): DIDSuffix = {
    require(DID.suffixRegex.pattern.matcher(didSuffix).matches(), s"invalid DID Suffix format: $didSuffix")
    new DIDSuffix(didSuffix)
  }
}
