package io.iohk.atala.prism

import com.google.protobuf.ByteString
import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.crypto.{Sha256, Sha256Digest}
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.identity.PrismDid.{getDEFAULT_MASTER_KEY_ID => masterKeyId}
import io.iohk.atala.prism.protos.node_api.{GetDidDocumentRequest, GetDidDocumentResponse}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.DIDData
import org.mockito.IdiomaticMockito._
import scalapb.GeneratedMessage
import identus.apollo.PublicKey

import scala.concurrent.Future
import identus.apollo.MyKeyPair
import identus.apollo.Secp256k1KeyPair

trait DIDUtil {
  protected def nodeMock: NodeService

  private def publicKeyToProto(key: PublicKey): node_models.ECKeyData = {
    val point = key.toCurvePoint
    node_models.ECKeyData(
      curve = key.curveName,
      x = ByteString.copyFrom(point.x),
      y = ByteString.copyFrom(point.y)
    )
  }

  def generateDid(masterPublicKey: PublicKey): DID = {
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
  ): (PublicKey, SignedRpcRequest[R]) = {
    val keys = Secp256k1KeyPair.generateKeyPair
    val did = generateDid(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def prepareSignedUnpublishedDidRequest[R <: GeneratedMessage](
      request: R
  ): (PublicKey, SignedRpcRequest[R]) = {
    val keys = MyKeyPair.generateKeyPair
    val did = DID.buildLongFormFromMasterPublicKey(keys.publicKey)
    (keys.publicKey, SignedRpcRequest.generate(keys, did, request))
  }

  def createDid: (KeyPair, DID) = {
    val keyPair = MyKeyPair.generateKeyPair
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
