package io.iohk.atala.prism.node.poc.endorsements

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.functor._
import com.google.protobuf.ByteString
import io.grpc.inprocess.{InProcessChannelBuilder, InProcessServerBuilder}
import io.grpc.{ManagedChannel, Server}
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.api.CredentialBatches
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECPublicKey
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.grpc.ProtoCodecs
import io.iohk.atala.prism.node.operations.ApplyOperationConfig
import io.iohk.atala.prism.node.poc.Wallet
import io.iohk.atala.prism.node.poc.endorsements.EndorsementsService.SignedKey
import io.iohk.atala.prism.node.repositories._
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.node.services._
import io.iohk.atala.prism.node.{DataPreparation, NodeGrpcServiceImpl, UnderlyingLedger}
import io.iohk.atala.prism.protos.endorsements_api.{
  EndorseInstitutionRequest,
  GetEndorsementsRequest,
  GetFreshMasterKeyRequest,
  RevokeEndorsementRequest
}
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, ScheduleOperationsRequest}
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.utils.NodeClientUtils.{issueBatchOperation, revokeCredentialsOperation}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.OptionValues.convertOptionToValuable
import tofu.logging.Logs

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

class EndorsementsFlowPoC extends AtalaWithPostgresSpec with BeforeAndAfterEach {
  import Utils._

  private val endorsementsFlowPoCLogs =
    Logs.withContext[IO, IOWithTraceIdContext]
  protected var serverName: String = _
  protected var serverHandle: Server = _
  protected var channelHandle: ManagedChannel = _
  protected var nodeServiceStub: node_api.NodeServiceGrpc.NodeServiceBlockingStub = _
  protected var didDataRepository: DIDDataRepository[IOWithTraceIdContext] = _
  protected var atalaOperationsRepository: AtalaOperationsRepository[IOWithTraceIdContext] = _
  protected var atalaObjectsTransactionsRepository: AtalaObjectsTransactionsRepository[IOWithTraceIdContext] = _
  protected var keyValuesRepository: KeyValuesRepository[IOWithTraceIdContext] =
    _
  protected var credentialBatchesRepository: CredentialBatchesRepository[IOWithTraceIdContext] = _
  protected var atalaReferenceLedger: UnderlyingLedger[IOWithTraceIdContext] = _
  protected var blockProcessingService: BlockProcessingServiceImpl = _
  protected var objectManagementService: ObjectManagementService[IOWithTraceIdContext] = _
  protected var metricsCountersRepository: MetricsCountersRepository[IOWithTraceIdContext] = _
  protected var submissionService: SubmissionService[IOWithTraceIdContext] = _
  protected var submissionSchedulingService: SubmissionSchedulingService = _
  protected var protocolVersionsRepository: ProtocolVersionRepository[IOWithTraceIdContext] = _
  private val publicKeysLimit = 10

  override def beforeEach(): Unit = {
    super.beforeEach()

    didDataRepository = DIDDataRepository.unsafe(dbLiftedToTraceIdIO, endorsementsFlowPoCLogs)
    credentialBatchesRepository = CredentialBatchesRepository.unsafe(
      dbLiftedToTraceIdIO,
      endorsementsFlowPoCLogs
    )
    protocolVersionsRepository = ProtocolVersionRepository.unsafe(
      dbLiftedToTraceIdIO,
      endorsementsFlowPoCLogs
    )

    atalaReferenceLedger = InMemoryLedgerService.unsafe(onAtalaReference, endorsementsFlowPoCLogs)
    blockProcessingService = new BlockProcessingServiceImpl(ApplyOperationConfig(DidSuffix("0a1e3")))
    atalaOperationsRepository = AtalaOperationsRepository.unsafe(
      dbLiftedToTraceIdIO,
      endorsementsFlowPoCLogs
    )
    metricsCountersRepository = MetricsCountersRepository.unsafe(dbLiftedToTraceIdIO, endorsementsFlowPoCLogs)
    atalaObjectsTransactionsRepository = AtalaObjectsTransactionsRepository
      .unsafe(dbLiftedToTraceIdIO, endorsementsFlowPoCLogs)
    keyValuesRepository = KeyValuesRepository.unsafe(dbLiftedToTraceIdIO, endorsementsFlowPoCLogs)
    objectManagementService = ObjectManagementService.unsafe(
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      keyValuesRepository,
      protocolVersionsRepository,
      blockProcessingService,
      publicKeysLimit,
      dbLiftedToTraceIdIO,
      endorsementsFlowPoCLogs
    )
    def onAtalaReference(notification: AtalaObjectNotification): IOWithTraceIdContext[Unit] =
      objectManagementService
        .saveObject(notification)
        .void

    submissionService = SubmissionService.unsafe(
      atalaReferenceLedger,
      atalaOperationsRepository,
      atalaObjectsTransactionsRepository,
      logs = endorsementsFlowPoCLogs
    )
    // this service needs to pull operations from the database and to send them to the ledger
    submissionSchedulingService = SubmissionSchedulingService(
      SubmissionSchedulingService.Config(
        refreshAndSubmitPeriod = 1.second,
        moveScheduledToPendingPeriod = 2.second
      ),
      submissionService
    )

    serverName = InProcessServerBuilder.generateName()

    serverHandle = InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(
        node_api.NodeServiceGrpc
          .bindService(
            new NodeGrpcServiceImpl(
              NodeService.unsafe(
                didDataRepository,
                objectManagementService,
                credentialBatchesRepository,
                publicKeysLimit,
                endorsementsFlowPoCLogs
              )
            ),
            executionContext
          )
      )
      .build()
      .start()

    channelHandle = InProcessChannelBuilder.forName(serverName).directExecutor().build()

    nodeServiceStub = node_api.NodeServiceGrpc.blockingStub(channelHandle)
  }

  override def afterEach(): Unit = {
    channelHandle.shutdown()
    channelHandle.awaitTermination(10, TimeUnit.SECONDS)
    serverHandle.shutdown()
    serverHandle.awaitTermination()
    super.afterEach()
  }

  "The batch issuance/verification flow" should {
    "work" in {

      val endorsementsService = EndorsementsService(nodeServiceStub)
      // we will make use of the toy wallet already implemented
      val wallet = Wallet(nodeServiceStub)

      // the steps of the flow to implement
      //  1. the MoE generates its DID
      val (moeDIDSuffix, createDIDOp) = wallet.generateDID()
      val moeDID = DID.fromString(s"did:prism:${moeDIDSuffix.getValue}")
      val signedAtalaOperation =
        wallet.signOperation(createDIDOp, "master0", moeDIDSuffix)
      val createDIDResponse = nodeServiceStub
        .scheduleOperations(
          ScheduleOperationsRequest(List(signedAtalaOperation))
        )
        .outputs
        .head

      // 2. We create 100 signed keys (we will later define how to derive them properly)
      val issuanceKeyId = "issuance0"
      val signedKeys: List[SignedKey] = (1 to 100).toList.map { _ =>
        val keyPair = EC.generateKeyPair()
        val publicKey = keyPair.getPublicKey
        SignedKey(
          publicKey,
          wallet.signKey(publicKey, issuanceKeyId, moeDIDSuffix),
          issuanceKeyId
        )
      }

      //  3. we initialize the endorsements service. This registers the MoE DID and
      //     a set of signed public keys
      endorsementsService
        .initialize(moeDID, signedKeys)
        .futureValue

      //  4. the MoE requests a master key the region
      val freshKeyProto = endorsementsService
        .getFreshMasterKey(
          GetFreshMasterKeyRequest()
            .withEndorserDID(moeDID.getValue)
        )
        .futureValue

      val retrievedKey =
        SignedKey(
          fromProtoKeyData(freshKeyProto.getKey),
          new ECSignature(freshKeyProto.signature.toByteArray),
          freshKeyProto.signingKeyId
        )

      DataPreparation.waitConfirmation(
        nodeServiceStub,
        createDIDResponse.getOperationId
      )
      //  5. the MoE validates the key signature
      val moeIssuingKey = ProtoCodecs
        .fromProtoKey(
          nodeServiceStub
            .getDidDocument(
              GetDidDocumentRequest(moeDID.getValue)
            )
            .getDocument
            .publicKeys
            .find(_.id == retrievedKey.signingKeyId)
            .get
        )
        .get

      wallet
        .verifySignedKey(
          retrievedKey.key,
          retrievedKey.signature,
          moeIssuingKey
        ) mustBe true

      //  6. the MoE shares the key with the region to endorse
      //  7. the region first generates its DID, then create a DID update
      //     that adds the key shared by the MoE as master key, and removes
      //     the original master key of the DID
      val (regionDIDSuffix, regionCreateDIDOp) = wallet.generateDID()
      val regionDID = DID.fromString(s"did:prism:${regionDIDSuffix.getValue}")
      val updateAddMoEKeyOp = updateDIDOp(
        Sha256.compute(regionCreateDIDOp.toByteArray),
        regionDIDSuffix,
        retrievedKey.key,
        "master0"
      )

      val signedRegionCreateDIDOp =
        wallet.signOperation(regionCreateDIDOp, "master0", regionDIDSuffix)
      val signedAddKeyOp =
        wallet.signOperation(updateAddMoEKeyOp, "master0", regionDIDSuffix)

      // the region now published the CreateDID and UpdateDID operations
      val scheduleOperationsResponse = nodeServiceStub.scheduleOperations(
        ScheduleOperationsRequest(
          Seq(
            signedRegionCreateDIDOp,
            signedAddKeyOp
          )
        )
      )
      scheduleOperationsResponse.outputs.size must be(2)
      DataPreparation.waitConfirmation(
        nodeServiceStub,
        scheduleOperationsResponse.outputs.map(
          _.operationMaybe.operationId.value
        ): _*
      )

      //  8. the region shares back its DID
      //  9. the MoE generates an endorsements credential and calls the endorsement RPC
      val credential =
        wallet.signCredential(
          {
            import kotlinx.serialization.json.JsonElementKt._
            import kotlinx.serialization.json.JsonObject
            val map = Map(
              "id" -> JsonPrimitive(moeDID.getValue),
              "keyId" -> JsonPrimitive(issuanceKeyId),
              "credentialSubject" -> JsonPrimitive(
                s"{'endorses': ${regionDID.getValue}"
              )
            )
            new CredentialContent(new JsonObject(map.asJava))
          },
          issuanceKeyId,
          moeDIDSuffix
        )

      val batch = CredentialBatches.batch(List(credential).asJava)
      val (root, proof) = (batch.getRoot, batch.getProofs.asScala.toList)
      val issueOp = issueBatchOperation(moeDID, root)
      val batchId = CredentialBatchId.fromBatchData(moeDIDSuffix.value, root)
      val issueOpHash = Sha256.compute(issueOp.toByteArray)
      val signedIssuanceOp =
        wallet.signOperation(issueOp, issuanceKeyId, moeDIDSuffix)
      endorsementsService
        .endorseInstitution(
          EndorseInstitutionRequest()
            .withParentDID(moeDID.getValue)
            .withChildDID(regionDID.getValue)
            .withCredential(credential.getCanonicalForm)
            .withEncodedMerkleProof(proof.head.encode)
            .withIssueBatch(signedIssuanceOp)
        )
        .futureValue

      // 11. we check the validity interval of the newly endorsed DID
      val validityInterval = endorsementsService
        .getEndorsements(
          GetEndorsementsRequest()
            .withDid(regionDID.getValue)
        )
        .futureValue

      println(validityInterval.toProtoString)

      // We revoke the endorsement
      val revocationKeyId = "revocation0"
      wallet.addRevocationKeyToDid(
        revocationKeyId = revocationKeyId,
        previousOperationHash = ByteString.copyFrom(Sha256.compute(createDIDOp.toByteArray).getValue),
        didSuffix = moeDIDSuffix
      )

      val revokeOp = revokeCredentialsOperation(issueOpHash, batchId)
      val signedRevokeOp =
        wallet.signOperation(revokeOp, revocationKeyId, moeDIDSuffix)
      endorsementsService
        .revokeEndorsement(
          RevokeEndorsementRequest()
            .withParentDID(moeDID.getValue)
            .withChildDID(regionDID.getValue)
            .withRevokeBatch(signedRevokeOp)
        )
        .futureValue

      // we check the validity interval of the un-endorsed DID
      val validityInterval2 = endorsementsService
        .getEndorsements(
          GetEndorsementsRequest()
            .withDid(regionDID.getValue)
        )
        .futureValue

      println(validityInterval2.toProtoString)
    }
  }
}

object Utils {

  def fromProtoKeyData(keyData: node_models.ECKeyData): ECPublicKey = {
    EC.toPublicKeyFromByteCoordinates(
      keyData.x.toByteArray,
      keyData.y.toByteArray
    )
  }

  def updateDIDOp(
      previousHash: Sha256Digest,
      suffix: DidSuffix,
      keyToAdd: ECPublicKey,
      keyIdToRemove: String
  ): node_models.AtalaOperation = {
    node_models.AtalaOperation(
      operation = node_models.AtalaOperation.Operation.UpdateDid(
        node_models.UpdateDIDOperation(
          previousOperationHash = ByteString.copyFrom(previousHash.getValue),
          id = suffix.getValue,
          actions = Seq(
            node_models.UpdateDIDAction(
              node_models.UpdateDIDAction.Action.AddKey(
                node_models.AddKeyAction(
                  key = Some(
                    node_models.PublicKey(
                      id = "masterMoE",
                      usage = node_models.KeyUsage.MASTER_KEY,
                      keyData = node_models.PublicKey.KeyData
                        .EcKeyData(ProtoCodecs.toECKeyData(keyToAdd))
                    )
                  )
                )
              )
            ),
            node_models.UpdateDIDAction(
              node_models.UpdateDIDAction.Action.RemoveKey(
                node_models.RemoveKeyAction(
                  keyId = keyIdToRemove
                )
              )
            )
          )
        )
      )
    )
  }

}
