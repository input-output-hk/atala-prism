package io.iohk.atala.prism.e2e

import com.google.protobuf.ByteString
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
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

abstract class VdrTestUtils extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  protected val host: String = sys.env.getOrElse("PRISM_NODE_HOST", "localhost")
  protected val port: Int = sys.env.getOrElse("PRISM_NODE_PORT", "50053").toInt

  protected var channel: ManagedChannel = _
  protected var client: NodeServiceBlockingStub = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    client = NodeServiceGrpc.blockingStub(channel)
  }

  override protected def afterAll(): Unit = {
    if (channel != null) {
      channel.shutdown()
      channel.awaitTermination(5, TimeUnit.SECONDS)
    }
    super.afterAll()
  }

  protected case class SecpPair(publicKey: SecpPublicKey, privateKey: SecpPrivateKey)

  protected def generateKeyPair(): SecpPair = {
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

  protected def compressedKeyData(pub: SecpPublicKey): node_models.CompressedECKeyData =
    node_models.CompressedECKeyData(
      curve = pub.curveName,
      data = ByteString.copyFrom(pub.compressed.toArray)
    )

  protected def buildCreateDid(master: SecpPair, vdrOpt: Option[(SecpPublicKey, String)]): node_models.AtalaOperation =
    node_models
      .AtalaOperation()
      .withCreateDid(
        node_models.CreateDIDOperation(
          didData = Some(
            node_models.CreateDIDOperation.DIDCreationData(
              publicKeys =
                List(
                  node_models.PublicKey(
                    id = "master",
                    usage = node_models.KeyUsage.MASTER_KEY,
                    keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(compressedKeyData(master.publicKey))
                  )
                ) ++ vdrOpt
                  .map { case (pub, curveName) =>
                    node_models.PublicKey(
                      id = "vdr",
                      usage = node_models.KeyUsage.VDR_KEY,
                      keyData = node_models.PublicKey.KeyData.CompressedEcKeyData(
                        node_models.CompressedECKeyData(
                          curve = curveName,
                          data = ByteString.copyFrom(pub.compressed.toArray)
                        )
                      )
                    )
                  }
                  .toList,
              services = Nil,
              context = Nil
            )
          )
        )
      )

  protected def signOperation(
      operation: node_models.AtalaOperation,
      keyId: String,
      key: SecpPrivateKey
  ): node_models.SignedAtalaOperation =
    node_models.SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(operation),
      signature = ByteString.copyFrom(SecpECDSA.signBytes(operation.toByteArray, key).bytes)
    )

  protected def operationIdOrFail(output: node_api.OperationOutput): ByteString =
    output.operationMaybe.operationId
      .orElse(output.operationMaybe.error.map(e => fail(s"Operation scheduling failed: $e")))
      .getOrElse(fail("Operation scheduling missing id and error"))

  protected def awaitApplied(operationId: ByteString, max: FiniteDuration = 90.seconds): common_models.OperationStatus = {
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

  protected def awaitRejected(operationId: ByteString, max: FiniteDuration = 90.seconds): common_models.OperationStatus = {
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
          other
      }
    }
    val finalStatus = loop()
    withClue(s"Final status for $operationId: $finalStatus") {
      finalStatus should not be common_models.OperationStatus.CONFIRMED_AND_APPLIED
    }
    finalStatus
  }

  protected def awaitRejectedOrPending(
      operationId: ByteString,
      max: FiniteDuration = 120.seconds
  ): common_models.OperationStatus = {
    val deadline = max.fromNow
    @tailrec
    def loop(): common_models.OperationStatus = {
      val statusResp = client.getOperationInfo(node_api.GetOperationInfoRequest(operationId))
      statusResp.operationStatus match {
        case common_models.OperationStatus.CONFIRMED_AND_REJECTED =>
          common_models.OperationStatus.CONFIRMED_AND_REJECTED
        case common_models.OperationStatus.CONFIRMED_AND_APPLIED =>
          fail(s"Operation unexpectedly applied: ${statusResp.details}")
        case common_models.OperationStatus.PENDING_SUBMISSION if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case common_models.OperationStatus.PENDING_SUBMISSION =>
          common_models.OperationStatus.PENDING_SUBMISSION
        case _ if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case other =>
          other
      }
    }
    loop()
  }

  protected def awaitFinalOrPending(
      operationId: ByteString,
      max: FiniteDuration = 120.seconds
  ): common_models.OperationStatus = {
    val deadline = max.fromNow
    @tailrec
    def loop(): common_models.OperationStatus = {
      val statusResp = client.getOperationInfo(node_api.GetOperationInfoRequest(operationId))
      statusResp.operationStatus match {
        case common_models.OperationStatus.CONFIRMED_AND_APPLIED |
            common_models.OperationStatus.CONFIRMED_AND_REJECTED =>
          statusResp.operationStatus
        case common_models.OperationStatus.PENDING_SUBMISSION if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case common_models.OperationStatus.PENDING_SUBMISSION =>
          common_models.OperationStatus.PENDING_SUBMISSION
        case _ if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case other =>
          other
      }
    }
    loop()
  }

  protected def awaitFinal(operationId: ByteString, max: FiniteDuration = 90.seconds): common_models.OperationStatus = {
    val deadline = max.fromNow
    @tailrec
    def loop(): common_models.OperationStatus = {
      val statusResp = client.getOperationInfo(node_api.GetOperationInfoRequest(operationId))
      statusResp.operationStatus match {
        case common_models.OperationStatus.CONFIRMED_AND_APPLIED |
            common_models.OperationStatus.CONFIRMED_AND_REJECTED =>
          statusResp.operationStatus
        case _ if deadline.hasTimeLeft() =>
          Thread.sleep(2000)
          loop()
        case other =>
          fail(s"Operation did not reach terminal state in time, last status: $other, details: ${statusResp.details}")
      }
    }
    loop()
  }

  protected def requireOutput(opt: Option[node_api.OperationOutput], ctx: String): node_api.OperationOutput =
    opt.getOrElse(fail(s"Missing operation output for $ctx"))

  protected def require[A](opt: Option[A], ctx: String): A =
    opt.getOrElse(fail(s"Missing $ctx"))

  protected def createDidWithVdrKey(master: SecpPair, vdr: SecpPair): Sha256Hash = {
    val createDidOp = buildCreateDid(master, Some((vdr.publicKey, vdr.publicKey.curveName)))
    val signedCreateDid = signOperation(createDidOp, "master", master.privateKey)
    val didScheduleResp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreateDid)))
    val didOpId = operationIdOrFail(didScheduleResp.outputs.head)
    awaitApplied(didOpId)
    Sha256Hash.compute(createDidOp.toByteArray)
  }

  protected def createDidWithCustomVdr(
      master: SecpPair,
      vdrPub: SecpPublicKey,
      curveOverride: String
  ): Sha256Hash = {
    val createDidOp = buildCreateDid(master, Some((vdrPub, curveOverride)))
    val signedCreateDid = signOperation(createDidOp, "master", master.privateKey)
    val didScheduleResp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreateDid)))
    val didOpId = operationIdOrFail(didScheduleResp.outputs.head)
    awaitApplied(didOpId)
    Sha256Hash.compute(createDidOp.toByteArray)
  }

  protected def createDidWithoutVdr(master: SecpPair): Sha256Hash = {
    val createDidOp = buildCreateDid(master, None)
    val signedCreateDid = signOperation(createDidOp, "master", master.privateKey)
    val didScheduleResp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signedCreateDid)))
    val didOpId = operationIdOrFail(didScheduleResp.outputs.head)
    awaitApplied(didOpId)
    Sha256Hash.compute(createDidOp.toByteArray)
  }

  protected def removeVdrKeyFromDid(didHash: Sha256Hash, master: SecpPair): Unit = {
    val updateOp = node_models.AtalaOperation().withUpdateDid(
      node_models.UpdateDIDOperation(
        previousOperationHash = ByteString.copyFrom(didHash.bytes.toArray),
        id = didHash.hexEncoded,
        actions = Seq(node_models.UpdateDIDAction().withRemoveKey(node_models.RemoveKeyAction(keyId = "vdr")))
      )
    )
    val signed = signOperation(updateOp, "master", master.privateKey)
    val resp = client.scheduleOperations(node_api.ScheduleOperationsRequest(Seq(signed)))
    val opId = operationIdOrFail(resp.outputs.head)
    val _ = awaitApplied(opId)
  }

  protected def createVdrEntry(
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

  protected def updateVdrEntry(
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
}
