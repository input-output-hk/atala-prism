package io.iohk.crypto

import java.security.{PrivateKey, PublicKey, Signature}

import org.bouncycastle.jce.provider.BouncyCastleProvider

object ECSignature {

  java.security.Security.addProvider(new BouncyCastleProvider)

  private val SIGNATURE_ALGORITHM = "SHA256withECDSA"

  def sign(privateKey: PrivateKey, text: String): Vector[Byte] = {
    val signer = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
    signer.initSign(privateKey)
    signer.update(text.getBytes("UTF-8"))
    val signature = signer.sign()
    signature.toVector
  }

  def verify(publicKey: PublicKey, text: String, signature: Vector[Byte]): Boolean = {

    val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
    verifier.initVerify(publicKey)
    verifier.update(text.getBytes("UTF-8"))
    verifier.verify(signature.toArray)
  }
}
