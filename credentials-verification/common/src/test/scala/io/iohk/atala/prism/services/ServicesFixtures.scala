package io.iohk.atala.prism.services

import java.time.Instant

import io.circe.Encoder
import io.circe.syntax._
import com.google.protobuf.ByteString

import io.iohk.atala.prism.credentials.{SlayerCredentialId, TimestampInfo}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api.GetCredentialStateResponse
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.EcKeyData
import io.iohk.atala.prism.protos.node_models.{DIDData, KeyUsage, PublicKey}
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.credentials.Credential
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.services.NodeClientService
import io.iohk.atala.prism.stubs.NodeClientServiceStub

trait ServicesFixtures {

  private implicit def ec = EC

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = DID.buildPrismDID("123456678abcdefg")

    val keyAddedDate: TimestampInfo = TimestampInfo(Instant.now().minusSeconds(1), 1, 1)
    val credentialIssueDate: TimestampInfo = TimestampInfo(Instant.now(), 2, 2)

    val keys: ECKeyPair = EC.generateKeyPair()
    val publicKey: PublicKey = PublicKey(
      id = issuanceKeyId,
      usage = KeyUsage.AUTHENTICATION_KEY,
      addedOn = Some(NodeClientService.toInfoProto(keyAddedDate)),
      revokedOn = None,
      keyData = EcKeyData(NodeClientService.toTimestampInfoProto(keys.publicKey))
    )

    val didData: DIDData = DIDData("", Seq(publicKey))
    val getCredentialStateResponse: GetCredentialStateResponse =
      GetCredentialStateResponse(
        issuerDID = issuerDID.value,
        publicationDate = Some(NodeClientService.toInfoProto(credentialIssueDate)),
        revocationDate = None
      )

    val nodeCredentialId1: SlayerCredentialId = SlayerCredentialId
      .compute(
        credentialHash = SHA256Digest.compute(jsonBasedCredential1.canonicalForm.getBytes),
        did = issuerDID
      )

    val nodeCredentialId2: SlayerCredentialId = SlayerCredentialId
      .compute(
        credentialHash = SHA256Digest.compute(jsonBasedCredential2.canonicalForm.getBytes),
        did = issuerDID
      )

    val defaultNodeClientStub: NodeClientServiceStub =
      new NodeClientServiceStub(
        Map(issuerDID -> didData),
        Map(
          nodeCredentialId1.string -> getCredentialStateResponse,
          nodeCredentialId2.string -> getCredentialStateResponse
        )
      )

    val rawMessage: ByteString = createRawMessage("{}")

    def createRawMessage(json: String): ByteString = {
      credential_models
        .Credential(typeId = "VerifiableCredential/RedlandIdCredential", credentialDocument = json)
        .toByteString
    }

    case class RedlandIdCredential(
        id: String,
        identityNumber: String,
        name: String,
        dateOfBirth: String
    )
    implicit lazy val redlandIdCredentialEncoder: Encoder[RedlandIdCredential] =
      Encoder.forProduct4("id", "identityNumber", "name", "dateOfBirth")(c =>
        (c.id, c.identityNumber, c.name, c.dateOfBirth)
      )

    lazy val redlandIdCredential1 = RedlandIdCredential(
      id = "id1",
      identityNumber = "identityNumber1",
      name = "name1",
      dateOfBirth = "1990-01-01"
    )

    lazy val redlandIdCredential2 = RedlandIdCredential(
      id = "id2",
      identityNumber = "identityNumber2",
      name = "name2",
      dateOfBirth = "1990-01-02"
    )

    lazy val jsonBasedCredential1 =
      Credential
        .fromCredentialContent(makeCredentialContent(redlandIdCredential1))
        .sign(keys.privateKey)

    lazy val jsonBasedCredential2 =
      Credential
        .fromCredentialContent(makeCredentialContent(redlandIdCredential2))
        .sign(keys.privateKey)

    def makeCredentialContent(redlandIdCredential: RedlandIdCredential): CredentialContent =
      CredentialContent(
        CredentialContent.JsonFields.CredentialType.field -> CredentialContent
          .Values("VerifiableCredential", "RedlandIdCredential"),
        CredentialContent.JsonFields.IssuerDid.field -> issuerDID.value,
        CredentialContent.JsonFields.IssuanceKeyId.field -> issuanceKeyId,
        CredentialContent.JsonFields.CredentialSubject.field -> redlandIdCredential.asJson.noSpaces
      )
  }
}
