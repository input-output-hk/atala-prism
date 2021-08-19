package io.iohk.atala.prism

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.identity.DID.masterKeyId
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, GetDidDocumentResponse}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import org.mockito.IdiomaticMockito._
import scalapb.GeneratedMessage

import scala.concurrent.Future

trait DIDUtil {
  protected def nodeMock: NodeService

  private def publicKeyToProto(key: ECPublicKey): node_models.ECKeyData = {
    val point = key.getCurvePoint
    node_models.ECKeyData(
      curve = ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

  def generateDid(masterPublicKey: ECPublicKey): DID = {
    val publicKey = node_models.PublicKey(
      id = masterKeyId,
      usage = node_models.KeyUsage.MASTER_KEY,
      keyData = node_models.PublicKey.KeyData.EcKeyData(
        publicKeyToProto(masterPublicKey)
      )
    )

    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = Seq(publicKey)
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = SHA256Digest.compute(operationBytes)
    val didCanonicalSuffix = operationHash.hexValue
    val did = DID.buildPrismDID(didCanonicalSuffix, null)

    nodeMock.getDidDocument(GetDidDocumentRequest(did.getValue)).returns {
      Future.successful(
        GetDidDocumentResponse(
          document = Some(DIDData(id = didCanonicalSuffix, publicKeys = Seq(publicKey)))
        )
      )
    }

    did
  }

  def prepareSignedRequest[R <: GeneratedMessage](request: R): (ECPublicKey, SignedRpcRequest[R]) = {
    val keys = EC.generateKeyPair()
    val did = generateDid(keys.getPublicKey)
    (keys.getPublicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def prepareSignedUnpublishedDidRequest[R <: GeneratedMessage](request: R): (ECPublicKey, SignedRpcRequest[R]) = {
    val keys = EC.generateKeyPair()
    val did = DID.createUnpublishedDID(keys.getPublicKey, null)
    (keys.getPublicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def createDid: (ECKeyPair, DID) = {
    val keyPair = EC.generateKeyPair()
    val publicKey = keyPair.getPublicKey
    val did = generateDid(publicKey)
    (keyPair, did)
  }

}

object DIDUtil {
  def createUnpublishedDid: (ECKeyPair, DID) = {
    val keyPair = EC.generateKeyPair()
    val publicKey = keyPair.getPublicKey
    val did = DID.createUnpublishedDID(publicKey, null)
    (keyPair, did)
  }
}
