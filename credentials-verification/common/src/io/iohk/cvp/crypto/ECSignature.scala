package io.iohk.cvp.crypto

import java.security.{PrivateKey, PublicKey, Signature}

import org.bouncycastle.jce.provider.BouncyCastleProvider

object ECSignature {

  java.security.Security.addProvider(new BouncyCastleProvider)

  private val SIGNATURE_ALGORITHM = "SHA256withECDSA"

  def sign(privateKey: PrivateKey, text: String): Vector[Byte] = {
    sign(privateKey, text.getBytes("UTF-8"))
  }

  def sign(privateKey: PrivateKey, data: Array[Byte]): Vector[Byte] = {
    val signer = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
    signer.initSign(privateKey)
    signer.update(data)
    val signature = signer.sign()
    signature.toVector
  }

  def verify(publicKey: PublicKey, text: String, signature: Vector[Byte]): Boolean = {
    verify(publicKey, text.getBytes("UTF-8"), signature)
  }

  def verify(publicKey: PublicKey, data: Array[Byte], signature: Vector[Byte]): Boolean = {

    val verifier = Signature.getInstance(SIGNATURE_ALGORITHM, "BC")
    verifier.initVerify(publicKey)
    verifier.update(data)
    verifier.verify(signature.toArray)
  }
}
