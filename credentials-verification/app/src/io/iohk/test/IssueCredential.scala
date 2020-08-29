package io.iohk.test

import io.iohk.atala.prism.crypto.{ECKeys, ECSignature}
import io.iohk.dids.DIDLoader
import javax.xml.bind.DatatypeConverter

import scala.util.{Failure, Success, Try}

object IssueCredential {

  def main(args: Array[String]): Unit = {
    val inputFilename = "certificate.txt"
    println("Loading issuer")
    val result = for {
      issuerDID <- DIDLoader.getDID(os.resource / "issuer" / "did.json")
      issuerJWKPrivateKey <- DIDLoader.getJWKPrivate(
        os.resource / "issuer" / "private.jwk"
      )
      issuerPrivateKey = ECKeys.toPrivateKey(issuerJWKPrivateKey.dBytes)

      _ = println("Loading subject")
      subjectDID <- DIDLoader.getDID(os.resource / "subject" / "did.json")

      _ = println("Loading the data to issue the credential")
      data <- Try {
        os.read.bytes(os.resource / inputFilename).toVector
      }
    } yield {
      val metadata = Credential.Metadata(
        issuedBy = issuerDID.id,
        subjectDID = subjectDID.id,
        issuedOn = System.currentTimeMillis(),
        data = data
      )

      println("Issuing the credential")
      val encodedMetadata = encode(metadata)
      val signature = ECSignature.sign(issuerPrivateKey, encodedMetadata)
      val credential = Credential(metadata = metadata, signature = signature)

      val outputFilename = s"$inputFilename.sig"
      println(s"Saving the credential to $outputFilename")
      val encodedCredential = encode(credential)
      os.write.over(os.pwd / outputFilename, encodedCredential)
    }

    result match {
      case Success(_) => println("done")
      case Failure(exception) =>
        exception.printStackTrace()
        println(exception.getMessage)
    }
  }

  // TODO: This must be a cross-language friendly format, possibly protobuf or JWS
  private def encode(metadata: Credential.Metadata): String = {
    s"""
      |{
      |  "issuedBy": "${metadata.issuedBy}",
      |  "issuedOn": ${metadata.issuedOn},
      |  "subjectDID": "${metadata.subjectDID}",
      |  "data": "${DatatypeConverter.printHexBinary(metadata.data.toArray)}"
      |}
    """.stripMargin.trim
  }

  // TODO: This must be a cross-language friendly format, possibly protobuf or JWS
  private def encode(credential: Credential): String = {
    s"""
       |{
       |  "metadata": ${encode(credential.metadata)},
       |  "signature": "${DatatypeConverter.printHexBinary(credential.signature.toArray)}"
       |}
     """.stripMargin.trim
  }
}
