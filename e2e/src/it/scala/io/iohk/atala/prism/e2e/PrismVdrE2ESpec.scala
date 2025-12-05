package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpECDSA, SecpPrivateKey, SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.protos.{common_models, node_api}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeServiceBlockingStub
import io.iohk.atala.prism.protos.node_models
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.params.{ECDomainParameters, ECKeyGenerationParameters, ECPrivateKeyParameters, ECPublicKeyParameters}
import org.bouncycastle.jce.ECNamedCurveTable
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import scala.annotation.tailrec
import scala.concurrent.duration._

class PrismVdrE2ESpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private val host = sys.env.getOrElse("PRISM_NODE_HOST", "localhost")
  private val port = sys.env.getOrElse("PRISM_NODE_PORT", "50053").toInt

  private var channel: ManagedChannel = _
  private var client: NodeServiceBlockingStub = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    client = NodeServiceGrpc.blockingStub(channel)
  }

  override def afterAll(): Unit = {
    if (channel != null) {
      channel.shutdown()
      channel.awaitTermination(5, TimeUnit.SECONDS)
    }
    super.afterAll()
  }

  "VDR gRPC flow" should {
    "create a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val createStorageOp = node_models
        .AtalaOperation()
        .withCreateStorageEntry(
          node_models
            .CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload-1")))
        )
      val signedCreateStorage = signOperation(createStorageOp, "vdr", vdr.privateKey)
      val createVdrResp =
        client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signedCreateStorage)))

      val createVdrOutput = requireOutput(createVdrResp.output, "create VDR")
      val createVdrOpId = operationIdOrFail(createVdrOutput)
      val createEventHash = require(createVdrOutput.result.createVdrEntryOutput, "create VDR event hash").eventHash
      awaitApplied(createVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val createdEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(createEventHash)).entry,
        "created entry"
      )
      createdEntry.deactivated shouldBe false
      createdEntry.data.flatMap(_.content.bytes) shouldBe Some(ByteString.copyFromUtf8("payload-1"))
      createdEntry.nonce shouldBe ByteString.EMPTY
    }

    "update a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")

      val updateStorageOp = node_models
        .AtalaOperation()
        .withUpdateStorageEntry(
          node_models
            .UpdateStorageEntryOperation()
            .withPreviousEventHash(createEventHash)
            .withData(node_models.StorageData().withIpfsCid("cid-2"))
        )
      val signedUpdateStorage = signOperation(updateStorageOp, "vdr", vdr.privateKey)
      val updateResp =
        client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdateStorage)))

      val updateOutput = requireOutput(updateResp.output, "update VDR")
      val updateVdrOpId = operationIdOrFail(updateOutput)
      val updateEventHash = require(updateOutput.result.updateVdrEntryOutput, "update VDR event hash").eventHash
      awaitApplied(updateVdrOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val updatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(updateEventHash)).entry,
        "updated entry"
      )
      updatedEntry.deactivated shouldBe false
      updatedEntry.data.flatMap(_.content.ipfsCid) shouldBe Some("cid-2")
      updatedEntry.previousEventHash shouldBe createEventHash
    }

    "deactivate a VDR resource" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val (createEventHash, _) = createVdrEntry(didSuffixHash, vdr, "payload-1")
      val updateEventHash = updateVdrEntry(createEventHash, vdr, "cid-2")

      val deactivateStorageOp = node_models
        .AtalaOperation()
        .withDeactivateStorageEntry(
          node_models
            .DeactivateStorageEntryOperation()
            .withPreviousEventHash(updateEventHash)
        )
      val signedDeactivate = signOperation(deactivateStorageOp, "vdr", vdr.privateKey)
      val deactivateResp =
        client.deactivateVdrEntry(node_api.DeactivateVdrEntryRequest(Some(signedDeactivate)))

      val deactivateOutput = requireOutput(deactivateResp.output, "deactivate VDR")
      val deactivateOpId = operationIdOrFail(deactivateOutput)
      val deactivateEventHash =
        require(deactivateOutput.result.deactivateVdrEntryOutput, "deactivate VDR event hash").eventHash
      awaitApplied(deactivateOpId) shouldBe common_models.OperationStatus.CONFIRMED_AND_APPLIED

      val deactivatedEntry = require(
        client.getVdrEntry(node_api.GetVdrEntryRequest(deactivateEventHash)).entry,
        "deactivated entry"
      )
      deactivatedEntry.deactivated shouldBe true
      deactivatedEntry.previousEventHash shouldBe updateEventHash
    }

    "reject VDR create when signed with non-VDR key" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      val didSuffixHash = createDidWithVdrKey(master, vdr)

      val badSignedCreate = signOperation(
        node_models.AtalaOperation().withCreateStorageEntry(
          node_models.CreateStorageEntryOperation()
            .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("payload")))
        ),
        keyId = "master", // wrong usage
        key = master.privateKey
      )

      val tryResp: Either[StatusRuntimeException, node_api.CreateVdrEntryResponse] =
        try {
          Right(client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(badSignedCreate))))
        } catch {
          case ex: StatusRuntimeException => Left(ex)
        }

      tryResp match {
        case Left(ex) =>
          ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
        case Right(resp) =>
          // If it was scheduled, it must be rejected when applied.
          val out = requireOutput(resp.output, "create VDR with bad key")
          val opId = operationIdOrFail(out)
          awaitRejected(opId) shouldBe common_models.OperationStatus.CONFIRMED_AND_REJECTED
      }
    }

    "reject VDR update with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("deadbeef")
      val signedUpdate = signOperation(
        node_models.AtalaOperation().withUpdateStorageEntry(
          node_models.UpdateStorageEntryOperation()
            .withPreviousEventHash(bogusPrev)
            .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8("x")))
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      val ex = intercept[io.grpc.StatusRuntimeException] {
        client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdate)))
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }

    "reject VDR deactivate with unknown previous hash" taggedAs E2ETestTag in {
      val master = generateKeyPair()
      val vdr = generateKeyPair()
      createDidWithVdrKey(master, vdr)

      val bogusPrev = ByteString.copyFromUtf8("cafebabe")
      val signedDeactivate = signOperation(
        node_models.AtalaOperation().withDeactivateStorageEntry(
          node_models.DeactivateStorageEntryOperation().withPreviousEventHash(bogusPrev)
        ),
        keyId = "vdr",
        key = vdr.privateKey
      )

      val ex = intercept[io.grpc.StatusRuntimeException] {
        client.deactivateVdrEntry(node_api.DeactivateVdrEntryRequest(Some(signedDeactivate)))
      }
      ex.getStatus.getCode shouldBe io.grpc.Status.INVALID_ARGUMENT.getCode
    }
  }

  private def buildCreateDid(
      master: SecpPair,
      vdr: SecpPair
  ): node_models.AtalaOperation = {
    node_models
      .AtalaOperation()
      .withCreateDid(
        node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys = List(
                node_models.PublicKey(
                  id = "master",
                  usage = node_models.KeyUsage.MASTER_KEY,
                  keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(compressedKeyData(master.publicKey))
                ),
                node_models.PublicKey(
                  id = "vdr",
                  usage = node_models.KeyUsage.VDR_KEY,
                  keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(compressedKeyData(vdr.publicKey))
                )
              ),
              services = Nil,
              context = Nil
            )
          )
        )
      )
  }

  private def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: SecpPrivateKey
  ): node_models.SignedAtalaOperation = {
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(SecpECDSA.signBytes(operation.toByteArray, key).bytes)
    )
  }

  private def operationIdOrFail(output: node_api.OperationOutput): ByteString = {
    output.operationMaybe.operationId
      .orElse(output.operationMaybe.error.map(e => fail(s"Operation scheduling failed: $e")))
      .getOrElse(fail("Operation scheduling missing id and error"))
  }

  private def awaitApplied(operationId: ByteString, max: FiniteDuration = 90.seconds): common_models.OperationStatus = {
    val deadline = max.fromNow
    @tailrec
    def loop(): common_models.OperationStatus = {
      val statusResp = client.getOperationInfo(node_api.GetOperationInfoRequest(operationId))
      statusResp.operationStatus match {
        case common_models.OperationStatus.CONFIRMED_AND_APPLIED =>
          common_models.OperationStatus.CONFIRMED_AND_APPLIED
        case common_models.OperationStatus.CONFIRMED_AND_REJECTED =>
          fail(s"Operation rejected: ${statusResp.details}")
        case _ if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case other =>
          fail(s"Operation did not complete in time, last status: $other, details: ${statusResp.details}")
      }
    }
    loop()
  }

  private def awaitRejected(operationId: ByteString, max: FiniteDuration = 90.seconds): common_models.OperationStatus = {
    val deadline = max.fromNow
    @tailrec
    def loop(): common_models.OperationStatus = {
      val statusResp = client.getOperationInfo(node_api.GetOperationInfoRequest(operationId))
      statusResp.operationStatus match {
        case common_models.OperationStatus.CONFIRMED_AND_REJECTED =>
          common_models.OperationStatus.CONFIRMED_AND_REJECTED
        case common_models.OperationStatus.CONFIRMED_AND_APPLIED =>
          fail(s"Operation unexpectedly applied: ${statusResp.details}")
        case _ if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case other =>
          fail(s"Operation did not reach rejected status in time, last status: $other, details: ${statusResp.details}")
      }
    }
    loop()
  }

  private def createDidWithVdrKey(master: SecpPair, vdr: SecpPair): Sha256Hash = {
    val createDidOp = buildCreateDid(master, vdr)
    val signedCreateDid = signOperation(createDidOp, "master", master.privateKey)
    val didScheduleResp =
      client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreateDid)))
    val didOpId = operationIdOrFail(didScheduleResp.outputs.head)
    awaitApplied(didOpId)
    Sha256Hash.compute(createDidOp.toByteArray)
  }

  private def requireOutput(opt: Option[node_api.OperationOutput], ctx: String): node_api.OperationOutput =
    opt.getOrElse(fail(s"Missing operation output for $ctx"))

  private def require[A](opt: Option[A], ctx: String): A =
    opt.getOrElse(fail(s"Missing $ctx"))

  private def createVdrEntry(
      didSuffixHash: Sha256Hash,
      vdr: SecpPair,
      payload: String
  ): (ByteString, ByteString) = {
    val createStorageOp = node_models
      .AtalaOperation()
      .withCreateStorageEntry(
        node_models
          .CreateStorageEntryOperation()
          .withDidPrismHash(ByteString.copyFrom(didSuffixHash.bytes.toArray))
          .withData(node_models.StorageData().withBytes(ByteString.copyFromUtf8(payload)))
      )
    val signedCreateStorage = signOperation(createStorageOp, "vdr", vdr.privateKey)
    val createVdrResp = client.createVdrEntry(node_api.CreateVdrEntryRequest(Some(signedCreateStorage)))

    val createVdrOutput = requireOutput(createVdrResp.output, "create VDR")
    val createVdrOpId = operationIdOrFail(createVdrOutput)
    val createEventHash = require(createVdrOutput.result.createVdrEntryOutput, "create VDR event hash").eventHash
    awaitApplied(createVdrOpId)
    (createEventHash, createVdrOpId)
  }

  private def updateVdrEntry(
      previousEventHash: ByteString,
      vdr: SecpPair,
      ipfsCid: String
  ): ByteString = {
    val updateStorageOp = node_models
      .AtalaOperation()
      .withUpdateStorageEntry(
        node_models
          .UpdateStorageEntryOperation()
          .withPreviousEventHash(previousEventHash)
          .withData(node_models.StorageData().withIpfsCid(ipfsCid))
      )
    val signedUpdateStorage = signOperation(updateStorageOp, "vdr", vdr.privateKey)
    val updateResp = client.updateVdrEntry(node_api.UpdateVdrEntryRequest(Some(signedUpdateStorage)))

    val updateOutput = requireOutput(updateResp.output, "update VDR")
    val updateVdrOpId = operationIdOrFail(updateOutput)
    val updateEventHash = require(updateOutput.result.updateVdrEntryOutput, "update VDR event hash").eventHash
    awaitApplied(updateVdrOpId)
    updateEventHash
  }

  private case class SecpPair(publicKey: SecpPublicKey, privateKey: SecpPrivateKey)

  private def generateKeyPair(): SecpPair = {
    val params = ECNamedCurveTable.getParameterSpec("secp256k1")
    val curve = params.getCurve
    val domainParams = new ECDomainParameters(curve, params.getG, params.getN, params.getH)
    val secureRandom = new SecureRandom()
    val keyParams = new ECKeyGenerationParameters(domainParams, secureRandom)

    val generator = new ECKeyPairGenerator()
    generator.init(keyParams)

    val keyPair = generator.generateKeyPair()
    val privateKeyParams = keyPair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
    val publicKeyParams = keyPair.getPublic.asInstanceOf[ECPublicKeyParameters]

    val privateKeyBytes = privateKeyParams.getD.toByteArray
    val publicKeyBytes = publicKeyParams.getQ.getEncoded(true)

    SecpPair(
      SecpPublicKey.unsafeFromCompressed(publicKeyBytes.toVector),
      SecpPrivateKey.unsafeFromBytesCompressed(privateKeyBytes)
    )
  }

  private def compressedKeyData(pub: SecpPublicKey): node_models.CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = pub.curveName,
      data = ByteString.copyFrom(pub.compressed.toArray)
    )
}
