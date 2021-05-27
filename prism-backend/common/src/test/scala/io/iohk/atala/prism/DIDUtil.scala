package io.iohk.atala.prism

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECKeyPair, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.identity.DID.masterKeyId
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
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
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
    val did = DID.buildPrismDID(didCanonicalSuffix, None)

    nodeMock.getDidDocument(GetDidDocumentRequest(did.value)).returns {
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
    val did = generateDid(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def prepareSignedUnpublishedDidRequest[R <: GeneratedMessage](request: R): (ECPublicKey, SignedRpcRequest[R]) = {
    val keys = EC.generateKeyPair()
    val did = DID.createUnpublishedDID(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def createDid: (ECKeyPair, DID) = {
    val keyPair = EC.generateKeyPair()
    val publicKey = keyPair.publicKey
    val did = generateDid(publicKey)
    (keyPair, did)
  }

}

object DIDUtil {
  def createUnpublishedDid: (ECKeyPair, DID) = {
    val keyPair = EC.generateKeyPair()
    val publicKey = keyPair.publicKey
    val did = DID.createUnpublishedDID(publicKey)
    (keyPair, did)
  }
}
