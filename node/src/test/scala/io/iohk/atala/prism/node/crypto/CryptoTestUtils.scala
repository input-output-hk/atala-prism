package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.node.crypto.CryptoUtils.SecpPublicKey
import io.iohk.atala.prism.node.models.PublicKeyData

object CryptoTestUtils {

  def generateECPair(): ECKeyPair = EC.INSTANCE.generateKeyPair()

  def generatePublicKey(): ECPublicKey = generateECPair().getPublicKey

  def generatePublicKeyData(): PublicKeyData = toPublicKeyData(generatePublicKey())

  def getUnderlyingKey(secpKey: SecpPublicKey): ECPublicKey = EC.INSTANCE.toPublicKeyFromCompressed(secpKey.compressed)

  def toPublicKeyData(ecKey: ECPublicKey): PublicKeyData = {
    val ecK = CryptoUtils.unsafeToSecpPublicKeyFromCompressed(ecKey.getEncodedCompressed.toVector)
    PublicKeyData(
      ecK.curveName,
      ecK.compressed.toVector
    )
  }
}
