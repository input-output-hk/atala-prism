package io.iohk.atala.prism.node

import com.google.protobuf.ByteString
import io.iohk.atala.prism.node.auth.SignedRpcRequest
import io.iohk.atala.prism.node.identity.{PrismDid => DID}
import io.iohk.atala.prism.node.identity.PrismDid.{DEFAULT_MASTER_KEY_ID => masterKeyId}
import io.iohk.atala.prism.node.crypto.CryptoTestUtils
import io.iohk.atala.prism.node.crypto.CryptoTestUtils.SecpPair
import io.iohk.atala.prism.node.crypto.CryptoUtils.{SecpPublicKey, Sha256Hash}
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, GetDidDocumentResponse}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import org.mockito.IdiomaticMockito._
import scalapb.GeneratedMessage

import scala.concurrent.Future

trait DIDUtil {
  protected def nodeMock: NodeService

  private def publicKeyToProto(key: SecpPublicKey): node_models.ECKeyData = {
    node_models.ECKeyData(
      curve = key.curveName,
      x = ByteString.copyFrom(key.x),
      y = ByteString.copyFrom(key.y)
    )
  }

  def generateDid(masterPublicKey: SecpPublicKey): DID = {
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
    val operationHash = Sha256Hash.compute(operationBytes)
    val didCanonicalSuffix = operationHash.hexEncoded
    val did = CryptoTestUtils.buildCanonicalDID(Sha256Hash.fromHex(didCanonicalSuffix))

    nodeMock.getDidDocument(GetDidDocumentRequest(did.value)).returns {
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
  ): (SecpPublicKey, SignedRpcRequest[R]) = {
    val keys = CryptoTestUtils.generateKeyPair()
    val did = generateDid(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def prepareSignedUnpublishedDidRequest[R <: GeneratedMessage](
      request: R
  ): (SecpPublicKey, SignedRpcRequest[R]) = {
    val keys = CryptoTestUtils.generateKeyPair()
    val did = DID.buildLongFormFromMasterPublicKey(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def createDid: (SecpPair, DID) = {
    val keyPair = CryptoTestUtils.generateKeyPair()
    val publicKey = keyPair.publicKey
    val did = generateDid(publicKey)
    (keyPair, did)
  }

}

object DIDUtil {
  def createUnpublishedDid: (SecpPair, DID) = {
    val keyPair = CryptoTestUtils.generateKeyPair()
    val did = DID.buildLongFormFromMasterPublicKey(keyPair.publicKey)
    (keyPair, did)
  }
}
