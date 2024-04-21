package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.{EC, Sha256Digest}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.identity.{CanonicalPrismDid, PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPrivateKey, SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.node.models.PublicKeyData

object CryptoTestUtils {

  case class SecpPair(publicKey: SecpPublicKey, privateKey: SecpPrivateKey)

  object SecpPair {
    def fromECPair(ecPair: ECKeyPair): SecpPair = {
      SecpPair(
        SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(ecPair.getPublicKey.getEncodedCompressed.toVector),
        SecpPrivateKey.unsafefromBytesCompressed(ecPair.getPrivateKey.getEncoded)
      )
    }
  }

  def generateKeyPair(): SecpPair = {
    SecpPair.fromECPair(
      EC.INSTANCE.generateKeyPair()
    )
  }

  def generatePublicKey(): SecpPublicKey = generateKeyPair().publicKey

  def generatePublicKeyData(): PublicKeyData = toPublicKeyData(
    generatePublicKey()
  )

  def getUnderlyingKey(secpKey: SecpPublicKey): ECPublicKey = EC.INSTANCE.toPublicKeyFromCompressed(secpKey.compressed)

  def toPublicKeyData(ecKey: SecpPublicKey): PublicKeyData = {
    val ecK = SecpPublicKey.unsafeToSecpPublicKeyFromCompressed(ecKey.compressed.toVector)
    PublicKeyData(
      ecK.curveName,
      ecK.compressed.toVector
    )
  }

  def buildCanonicalDID(hash: Sha256Hash): CanonicalPrismDid = DID.buildCanonical(
    Sha256Digest.fromBytes(hash.bytes.toArray)
  )
}
