package io.iohk.test

import io.iohk.crypto.{ECKeys, ECSignature}
import io.iohk.dids.DIDLoader
import javax.xml.bind.DatatypeConverter

object IssueCredential {

  def main(args: Array[String]): Unit = {
    for {
      issuerDID <- DIDLoader.getDID(os.resource / "issuer" / "did.json")
      issuerJWKPrivateKey <- DIDLoader.getJWKPrivate(
        os.resource / "issuer" / "private.jwk"
      )

      subjectDID <- DIDLoader.getDID(os.resource / "subject" / "did.json")
      subjectJWKPrivateKey <- DIDLoader.getJWKPrivate(
        os.resource / "subject" / "private.jwk"
      )
    } yield {
      val issuerPrivateKey = ECKeys.toPrivateKey(issuerJWKPrivateKey.dBytes)
      val issuerPublicKey = ECKeys.toPublicKey(
        issuerDID.publicKey.head.publicKeyJwk.xBytes,
        issuerDID.publicKey.head.publicKeyJwk.yBytes
      )

      val text = "iohk"
      println(s"The issuer is signing the text '$text'")

      val signature = ECSignature.sign(issuerPrivateKey, text)
      val signatureHex = DatatypeConverter.printHexBinary(signature.toArray)
      println(s"Signature: $signatureHex")

      val verified = ECSignature.verify(issuerPublicKey, text, signature)
      println(
        s"Verifying the signature using the issuer's public key: $verified"
      )
    }
  }
}
