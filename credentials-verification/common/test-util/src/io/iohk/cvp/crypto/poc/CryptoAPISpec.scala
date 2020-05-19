package io.iohk.cvp.crypto.poc

import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.time.{Instant, LocalDate}
import java.util.UUID

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.crypto.ECKeys.EncodedPublicKey
import org.scalatest.{MustMatchers, WordSpec}

// This test displays the way to use the CryptoAPI
// We will show the use with

class CryptoAPISpec extends WordSpec with MustMatchers {
  "CryptoAPI" should {
    "Work with a toy credential" in {

      // As we don't have a concrete representation for
      // credentials. We define a toy example using JSON
      // structure

      // this is a model from the CManager we use to show the flow
      case class CmanagerCredential(
          id: UUID,
          issuedBy: UUID,
          studentId: UUID,
          title: String,
          enrollmentDate: LocalDate,
          graduationDate: LocalDate,
          groupName: String,
          createdOn: Instant,
          issuerName: String,
          studentName: String
      ) {
        def toJSON: String =
          s"""{
            |    "tittle" : "$title",
            |    "enrollmentDate" : "$enrollmentDate",
            |    "graduationDate" : "$graduationDate",
            |    "studentName" : "$studentName"
            |  }""".stripMargin
      }

      def generateCredential(credentialType: String, issuerDID: String, keyId: String, c: CmanagerCredential): String =
        s"""{
           |  "credentialType" : "$credentialType",
           |  "issuerDID" : "$issuerDID",
           |  "signingKey" : {
           |     "type" : "DIDKey",
           |     "key" : "$issuerDID#$keyId"
           |  },
           |  "claims" : ${c.toJSON}
           |}""".stripMargin

      val keyPair = ECKeys.generateKeyPair()
      val privateKey = keyPair.getPrivate
      val publicKey = keyPair.getPublic
      val issuerDID = "did:prism:issuer123"
      val credentialType = "university-degree"
      val keyId = "Issuing-00"
      val cmanagerCredential = CmanagerCredential(
        id = UUID.randomUUID(),
        issuedBy = UUID.randomUUID(),
        studentId = UUID.randomUUID(),
        title = "Bs in Computer Science",
        enrollmentDate = LocalDate.now(),
        graduationDate = LocalDate.now(),
        groupName = "Graduation COVID-19",
        createdOn = Instant.now(),
        issuerName = "National University of Rosario",
        studentName = "Asymptomatic Joe"
      )

      // some toy helper methods
      def getIssuer(credential: String): String = issuerDID
      def getKeyId(credential: String) = keyId
      def getKey(issuerDID: String, keyId: String) = publicKey

      // a tiny simulation of sending the credential
      var credentialChannel: String = null

      def sendCredential(c: SignedCredential): Unit = {
        credentialChannel = c.canonicalForm
      }

      def receivedCredential(): String = {
        credentialChannel
      }

      // the idea of the flow
      val credentialToSign = generateCredential(credentialType, issuerDID, keyId, cmanagerCredential)
      val credentialBytes: Array[Byte] = credentialToSign.getBytes(StandardCharsets.UTF_8)

      println(s"We created a credential: $credentialToSign")
      println(s"The credential bytes are: $credentialBytes")

      val api = CryptoAPIImpl

      val signedCredential = api.sign(privateKey, credentialBytes)
      println(s"We signed the credential and obtain: ${signedCredential.canonicalForm}")

      println(s"we now send the credential to another person")
      sendCredential(signedCredential)

      println("the other person received the credential")
      val verifyMe = SignedCredential.from(receivedCredential()).get
      println(s"Thre credential received is $verifyMe")

      println("We decompose the signed credential into credential bytes and signature bytes")
      val (cred, _) = SignedCredential.decode(verifyMe)

      println("we reconstruct the credential representation")
      val concreteRepresentation = new String(cred, StandardCharsets.UTF_8)
      println(s"Hi, it is me again: $concreteRepresentation")

      // We would extract issuer DID and verify we trust the DID
      val issuer = getIssuer(concreteRepresentation)
      // we would extract the issuer key
      val issuingKeyId = getKeyId(concreteRepresentation)

      val verificationKey = getKey(issuer, issuingKeyId)
      println(s"we extracted the key: $verificationKey")

      println(s"we finally check the signature")
      api.verify(verificationKey, verifyMe) mustBe true

      println(s"We also compute the signed credential hash: ${api.hash(verifyMe)}")
    }
  }
}
