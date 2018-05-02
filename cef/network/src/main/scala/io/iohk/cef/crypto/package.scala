package io.iohk.cef

import java.security.SecureRandom

import akka.util.ByteString
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{ECDomainParameters, ECKeyGenerationParameters, ECPrivateKeyParameters, ECPublicKeyParameters}

package object crypto {
  val curveParams: X9ECParameters = SECNamedCurves.getByName("secp256k1")
  val curve: ECDomainParameters = new ECDomainParameters(curveParams.getCurve, curveParams.getG, curveParams.getN, curveParams.getH)


  def kec256(input: Array[Byte]*): Array[Byte] = {
    val digest = new KeccakDigest(256)
    val output = Array.ofDim[Byte](digest.getDigestSize)
    input.foreach(i => digest.update(i, 0, i.length))
    digest.doFinal(output, 0)
    output
  }

  def kec256(input: ByteString): ByteString =
    ByteString(kec256(input.toArray))

  def generateKeyPair(secureRandom: SecureRandom): AsymmetricCipherKeyPair = {
    val generator = new ECKeyPairGenerator
    generator.init(new ECKeyGenerationParameters(curve, secureRandom))
    generator.generateKeyPair()
  }

  /** @return (privateKey, publicKey) pair */
  def keyPairToByteArrays(keyPair: AsymmetricCipherKeyPair): (Array[Byte], Array[Byte]) = {
    val prvKey = keyPair.getPrivate.asInstanceOf[ECPrivateKeyParameters].getD.toByteArray
    val pubKey = keyPair.getPublic.asInstanceOf[ECPublicKeyParameters].getQ.getEncoded(false).tail
    (prvKey, pubKey)
  }

  def keyPairFromPrvKey(prvKeyBytes: Array[Byte]): AsymmetricCipherKeyPair = {
    val privateKey = BigInt(1, prvKeyBytes)
    keyPairFromPrvKey(privateKey)
  }

  def keyPairFromPrvKey(prvKey: BigInt): AsymmetricCipherKeyPair = {
    val publicKey = curve.getG.multiply(prvKey.bigInteger).normalize()
    new AsymmetricCipherKeyPair(new ECPublicKeyParameters(publicKey, curve), new ECPrivateKeyParameters(prvKey.bigInteger, curve))
  }
}
