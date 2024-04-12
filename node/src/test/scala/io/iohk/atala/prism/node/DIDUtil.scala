package io.iohk.atala.prism.node

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.{ECKeyPair, ECPublicKey}
import io.iohk.atala.prism.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.identity.PrismDid.{getDEFAULT_MASTER_KEY_ID => masterKeyId}
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
        node_models.CreateDIDOperation.DIDCreationData(
          publicKeys = Seq(publicKey)
        )
      )
    )

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val operationBytes = atalaOp.toByteArray
    val operationHash = Sha256.compute(operationBytes)
    val didCanonicalSuffix = operationHash.getHexValue
    val did = DID.buildCanonical(Sha256Digest.fromHex(didCanonicalSuffix))

    nodeMock.getDidDocument(GetDidDocumentRequest(did.getValue)).returns {
      Future.successful(
        GetDidDocumentResponse(
          document = Some(DIDData(id = didCanonicalSuffix, publicKeys = Seq(publicKey)))
        )
      )
    }

    did
  }

  def prepareSignedRequest[R <: GeneratedMessage](
      request: R
  ): (ECPublicKey, SignedRpcRequest[R]) = {
    val keys = EC.generateKeyPair()
    val did = generateDid(keys.getPublicKey)
    (keys.getPublicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def prepareSignedUnpublishedDidRequest[R <: GeneratedMessage](
      request: R
  ): (ECPublicKey, SignedRpcRequest[R]) = {
    val keys = EC.generateKeyPair()
    val did = DID.buildLongFormFromMasterPublicKey(keys.getPublicKey)
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
    val did = DID.buildLongFormFromMasterPublicKey(publicKey)
    (keyPair, did)
  }
}
