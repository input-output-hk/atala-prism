package io.iohk.atala.prism.node.crypto

import io.iohk.atala.prism.node.identity.{CanonicalPrismDid, PrismDid => DID}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPrivateKey, SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.node.models.PublicKeyData
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{
  ECDomainParameters,
  ECKeyGenerationParameters,
  ECPrivateKeyParameters,
  ECPublicKeyParameters
}
import org.bouncycastle.jce.ECNamedCurveTable

import java.security.SecureRandom

object CryptoTestUtils {

  case class SecpPair(publicKey: SecpPublicKey, privateKey: SecpPrivateKey)

  object SecpPair {
    def fromECPair(publicCompressed: Array[Byte], privateEncoded: Array[Byte]): SecpPair = {
      SecpPair(
        SecpPublicKey.unsafeFromCompressed(publicCompressed.toVector),
        SecpPrivateKey.unsafeFromBytesCompressed(privateEncoded)
      )
    }
  }

  def generateKeyPair(): SecpPair = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val curve = params.getCurve
    val domainParams = new ECDomainParameters(curve, params.getG, params.getN, params.getH)
    val secureRandom = new SecureRandom()
    val keyParams = new ECKeyGenerationParameters(domainParams, secureRandom)

    val generator = new ECKeyPairGenerator()
    generator.init(keyParams)

    val keyPair = generator.generateKeyPair()
    val privateKeyParams = keyPair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
    val publicKeyParams = keyPair.getPublic.asInstanceOf[ECPublicKeyParameters]

    val privateKeyBytes = privateKeyParams.getD.toByteArray
    val publicKeyBytes = publicKeyParams.getQ.getEncoded(true)

    SecpPair.fromECPair(publicKeyBytes, privateKeyBytes)
  }

  def generatePublicKeyData(): PublicKeyData = toPublicKeyData(
    generateKeyPair().publicKey
  )

  def toPublicKeyData(ecKey: SecpPublicKey): PublicKeyData = {
    val ecK = SecpPublicKey.unsafeFromCompressed(ecKey.compressed.toVector)
    PublicKeyData(
      ecK.curveName,
      ecK.compressed.toVector
    )
  }

  def buildCanonicalDID(hash: Sha256Hash): CanonicalPrismDid = DID.buildCanonical(hash)
}
