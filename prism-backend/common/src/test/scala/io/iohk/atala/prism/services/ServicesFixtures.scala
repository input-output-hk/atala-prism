package io.iohk.atala.prism.services

import cats.implicits.catsSyntaxOptionId

import java.time.Instant
import io.circe.Encoder
import io.circe.syntax._
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.extras.CredentialBatches
import io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, Sha256}
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.protos.node_models.TimestampInfo
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
import io.iohk.atala.prism.utils.syntax._

import scala.jdk.CollectionConverters._
import io.iohk.atala.prism.interop.CredentialContentConverter._
import io.iohk.atala.prism.utils.StringUtils.encodeToByteArray

trait ServicesFixtures {
  object ConnectorClientServiceFixtures {
    val defaultConnectorConfig = ConnectorConfig(
      host = "localhost",
      port = 9999
    )

    val defaultDidBasedAuthConfig = DidBasedAuthConfig(
      did = PrismDid.buildCanonical(Sha256.compute(encodeToByteArray( "did"))),
      didMasterKeyId = "master",
      didMasterKeyPair = EC.generateKeyPair(),
      didIssuingKeyId = "issuance",
      didIssuingKeyPair = EC.generateKeyPair()
    )
  }

  object CredentialFixtures {

    val issuanceKeyId = "Issuance-0"
    val issuerDID = newDID()

    val keyAddedDate: TimestampInfo = new TimestampInfo(1, 1, Instant.now().minusSeconds(1).toProtoTimestamp.some)
    val credentialIssueDate: TimestampInfo = new TimestampInfo(2, 2, Instant.now().toProtoTimestamp.some)

    val keys: ECKeyPair = EC.generateKeyPair()
    val publicKey: PublicKey = PublicKey(
      id = issuanceKeyId,
      usage = KeyUsage.AUTHENTICATION_KEY,
      addedOn = Some(LedgerData(timestampInfo = Some(NodeClientService.toInfoProto(keyAddedDate)))),
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
        "type" -> new JsonArray(
          List(JsonPrimitive("VerifiableCredential"), JsonPrimitive("RedlandIdCredential")).asJava
        ),
        "id" -> JsonPrimitive(issuerDID.getValue),
        "keyId" -> JsonPrimitive(issuanceKeyId),
        "credentialSubject" -> JsonPrimitive(redlandIdCredential.asJson.noSpaces)
      )

      new CredentialContent(new JsonObject(map.asJava))
    }
  }

  def newDID(): PrismDid = {
    PrismDid.buildLongFormFromMasterKey(EC.generateKeyPair().getPublicKey)
  }
}
