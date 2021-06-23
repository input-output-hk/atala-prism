package io.iohk.atala.prism.services

import java.time.Instant
import io.circe.Encoder
import io.circe.syntax._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials.{Credential, CredentialBatchId, CredentialBatches, TimestampInfo}
import io.iohk.atala.prism.crypto.{EC, ECKeyPair}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api.GetBatchStateResponse
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.EcKeyData
import io.iohk.atala.prism.protos.node_models.{DIDData, KeyUsage, LedgerData, PublicKey}
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.stubs.NodeClientServiceStub
import io.iohk.atala.prism.config.ConnectorConfig
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.protos.credential_models.PlainTextCredential
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import org.scalatest.OptionValues._

trait ServicesFixtures {

  private implicit def ec = EC

  object ConnectorClientServiceFixtures {
    val defaultConnectorConfig = ConnectorConfig(
      host = "localhost",
      port = 9999
    )

    val defaultDidBasedAuthConfig = DidBasedAuthConfig(
      did = DID.buildPrismDID("did"),
      didMasterKeyId = "master",
      didMasterKeyPair = EC.generateKeyPair(),
      didIssuingKeyId = "issuance",
      didIssuingKeyPair = EC.generateKeyPair()
    )
  }

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = newDID()

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

    val didData: DIDData = DIDData("", Seq(publicKey))

    val (root, List(proof1, proof2)) = CredentialBatches.batch(List(jsonBasedCredential1, jsonBasedCredential2))
    val credentialBatchId: CredentialBatchId = CredentialBatchId.fromBatchData(issuerDID.suffix, root)
    val getBatchStateResponse: GetBatchStateResponse =
      GetBatchStateResponse(
        issuerDid = issuerDID.value,
        merkleRoot = NodeClientService.toByteString(root.hash),
        publicationLedgerData = Some(
          LedgerData(
            timestampInfo = Some(NodeClientService.toInfoProto(credentialIssueDate))
          )
        ),
        revocationLedgerData = None
      )

    val defaultNodeClientStub: NodeClientServiceStub =
      new NodeClientServiceStub(
        didDocument = Map(issuerDID -> didData),
        getBatchStateResponse = Map(credentialBatchId -> getBatchStateResponse)
      )

    def plainTextCredentialMessage(
        credential: Credential,
        merkleInclusionProof: MerkleInclusionProof
    ): PlainTextCredential =
      PlainTextCredential(
        credential.canonicalForm,
        encodedMerkleProof = merkleInclusionProof.encode
      )

    def makeCredentialContent(redlandIdCredential: RedlandIdCredential): CredentialContent =
      CredentialContent(
        CredentialContent.JsonFields.CredentialType.field -> CredentialContent
          .Values("VerifiableCredential", "RedlandIdCredential"),
        CredentialContent.JsonFields.IssuerDid.field -> issuerDID.value,
        CredentialContent.JsonFields.IssuanceKeyId.field -> issuanceKeyId,
        CredentialContent.JsonFields.CredentialSubject.field -> redlandIdCredential.asJson.noSpaces
      )
  }

  def newDID(): DID = {
    DID.createUnpublishedDID(EC.generateKeyPair().publicKey).canonical.value
  }
}
