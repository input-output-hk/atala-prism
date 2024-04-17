package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.node.models.{DIDPublicKey, PublicKeyData}

object CryptoTestUtils {

  def generateECPair(): ECKeyPair = EC.INSTANCE.generateKeyPair()

  def generatePublicKey(): ECPublicKey = generateECPair().getPublicKey

  def generatePublicKeyData(): PublicKeyData = toPublicKeyData(generatePublicKey())

  def compareDIDPubKeys(k1:  DIDPublicKey, k2: DIDPublicKey): Boolean = {
    k1.didSuffix == k2.didSuffix &&
      k1.keyId == k2.keyId &&
      k1.keyUsage == k2.keyUsage &&
      k1.key.compressedKey.toVector == k2.key.compressedKey.toVector
  }

  def getUnderlyingKey(secpKey: SecpPublicKey): ECPublicKey = EC.INSTANCE.toPublicKeyFromCompressed(secpKey.compressed)

  def toPublicKeyData(ecKey: ECPublicKey): PublicKeyData = {
    val ecK = CryptoUtils.unsafeToSecpPublicKeyFromCompressed(ecKey.getEncodedCompressed)
    PublicKeyData(
      ecK.curveName,
      ecK.compressed
    )
  }
}
