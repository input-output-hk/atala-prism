package io.iohk.atala.prism.services

import java.time.Instant
import io.circe.Encoder
import io.circe.syntax._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.credentials.{CredentialBatchId, CredentialBatches, TimestampInfo}
import io.iohk.atala.prism.kotlin.crypto.{EC, MerkleInclusionProof}
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.protos.node_api.GetBatchStateResponse
import io.iohk.atala.prism.protos.node_models.PublicKey.KeyData.EcKeyData
import io.iohk.atala.prism.protos.node_models.{DIDData, KeyUsage, LedgerData, PublicKey}
import io.iohk.atala.prism.protos.credential_models
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.stubs.NodeClientServiceStub
import io.iohk.atala.prism.config.ConnectorConfig
import io.iohk.atala.prism.kotlin.credentials.PrismCredential
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.protos.credential_models.PlainTextCredential
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.utils.Base64Utils

import scala.jdk.CollectionConverters._
import io.iohk.atala.prism.interop.CredentialContentConverter._

trait ServicesFixtures {
  object ConnectorClientServiceFixtures {
    val defaultConnectorConfig = ConnectorConfig(
      host = "localhost",
      port = 9999
    )

    val defaultDidBasedAuthConfig = DidBasedAuthConfig(
      did = DID.buildPrismDID("did", null),
      didMasterKeyId = "master",
      didMasterKeyPair = EC.generateKeyPair(),
      didIssuingKeyId = "issuance",
      didIssuingKeyPair = EC.generateKeyPair()
    )
  }

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = newDID()

    val keyAddedDate: TimestampInfo = new TimestampInfo(Instant.now().minusSeconds(1).getEpochSecond, 1, 1)
    val credentialIssueDate: TimestampInfo = new TimestampInfo(Instant.now().getEpochSecond, 2, 2)

    val keys: ECKeyPair = EC.generateKeyPair()
    val publicKey: PublicKey = PublicKey(
      id = issuanceKeyId,
      usage = KeyUsage.AUTHENTICATION_KEY,
      addedOn = Some(NodeClientService.toInfoProto(keyAddedDate)),
      revokedOn = None,
      keyData = EcKeyData(NodeClientService.toTimestampInfoProto(keys.getPublicKey))
    )

    val rawMessage: ByteString = createRawMessage("{}")

    def createRawMessage(json: String): ByteString = {
      credential_models
        .PlainTextCredential(encodedCredential = Base64Utils.encodeURL(json.getBytes))
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

    lazy val jsonBasedCredential1: PrismCredential = {
      JsonBasedCredential
        .fromString(makeCredentialContent(redlandIdCredential1).asString)
        .sign(keys.getPrivateKey)
    }

    lazy val jsonBasedCredential2: PrismCredential = {
      JsonBasedCredential
        .fromString(makeCredentialContent(redlandIdCredential2).asString)
        .sign(keys.getPrivateKey)
    }

    val didData: DIDData = DIDData("", Seq(publicKey))

    val batch = CredentialBatches.batch(List(jsonBasedCredential1, jsonBasedCredential2).asJava)
    val (root, List(proof1, proof2)) = (batch.getRoot, batch.getProofs.asScala.toList)
    val credentialBatchId: CredentialBatchId = CredentialBatchId.fromBatchData(issuerDID.getSuffix, root)
    val getBatchStateResponse: GetBatchStateResponse =
      GetBatchStateResponse(
        issuerDid = issuerDID.getValue,
        merkleRoot = NodeClientService.toByteString(root.getHash),
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
        credential: PrismCredential,
        merkleInclusionProof: MerkleInclusionProof
    ): PlainTextCredential =
      PlainTextCredential(
        credential.getCanonicalForm,
        encodedMerkleProof = merkleInclusionProof.encode
      )

    def makeCredentialContent(redlandIdCredential: RedlandIdCredential): CredentialContent = {
      import kotlinx.serialization.json.JsonElementKt._
      import kotlinx.serialization.json.JsonArray
      import kotlinx.serialization.json.JsonObject

      val map = Map(
        "type" -> new JsonArray(List(JsonPrimitive("VerifiableCredential"), JsonPrimitive("RedlandIdCredential")).asJava),
        "id" -> JsonPrimitive(issuerDID.getValue),
        "keyId" -> JsonPrimitive(issuanceKeyId),
        "credentialSubject" -> JsonPrimitive(redlandIdCredential.asJson.noSpaces)
      )

      new CredentialContent(new JsonObject(map.asJava))
    }
  }

  def newDID(): DID = {
    DID.createUnpublishedDID(EC.generateKeyPair().getPublicKey, null)
    // where is the canon form getter?
  }
}
