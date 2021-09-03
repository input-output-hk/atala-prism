package io.iohk.atala.prism.services

import monix.eval.Task
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.kotlin.identity.PrismDid
import io.iohk.atala.prism.protos.{node_api, node_models}
import cats.implicits._
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot

trait NodeClientService {

  def getDidDocument(did: PrismDid): Task[Option[DIDData]]

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]]

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse]

  def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): Task[GetCredentialRevocationTimeResponse]

}

class NodeClientServiceImpl(node: NodeServiceGrpc.NodeServiceStub, authConfig: DidBasedAuthConfig)
    extends NodeClientService {

  def getDidDocument(did: PrismDid): Task[Option[DIDData]] =
    Task.fromFuture(node.getDidDocument(GetDidDocumentRequest(did.getValue))).map(_.document)

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]] =
    Task
      .fromFuture(node.getBatchState(GetBatchStateRequest(credentialBatchId.getId)))
      .map { response =>
        if (response.issuerDid.nonEmpty) Some(response)
        else None
      }

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse] = {
    val operation =
      NodeClientService.issueBatchOperation(authConfig.did, merkleRoot)

    val signedAtalaOperation =
      SignedAtalaOperation(
        signedWith = authConfig.didIssuingKeyId,
        operation = Some(operation),
        signature =
          ByteString.copyFrom(EC.signBytes(operation.toByteArray, authConfig.didIssuingKeyPair.getPrivateKey).getData)
      )

    Task.fromFuture(node.issueCredentialBatch(IssueCredentialBatchRequest().withSignedOperation(signedAtalaOperation)))
  }

  def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: Sha256Digest
  ): Task[GetCredentialRevocationTimeResponse] = {
    Task.fromFuture(
      node.getCredentialRevocationTime(
        node_api
          .GetCredentialRevocationTimeRequest()
          .withBatchId(credentialBatchId.getId)
          .withCredentialHash(ByteString.copyFrom(credentialHash.getValue))
      )
    )
  }

}
object NodeClientService {

//  def getKeyData(issuerDID: PrismDid, issuanceKeyId: String, nodeService: NodeClientService):
//  EitherT[Task, String, credentials.KeyData] = {
//    for {
//      didData <- EitherT(
//        nodeService.getDidDocument(issuerDID).map(_.toRight(s"DID Data not found for DID ${issuerDID.getValue}"))
//      )
//
//      issuingKeyProto <-
//        didData.publicKeys
//          .find(_.id == issuanceKeyId)
//          .toRight(s"KeyId not found: $issuanceKeyId")
//          .toEitherT[Task]
//
//      issuingKey <- fromProtoKey(issuingKeyProto)
//        .toRight(s"Failed to parse proto key: $issuingKeyProto")
//        .toEitherT[Task]
//
//      addedOn <-
//        issuingKeyProto.addedOn
//          .flatMap(ledgerData => ledgerData.timestampInfo)
//          .map(fromTimestampInfoProto)
//          .toRight(s"Missing addedOn time:\n-Issuer DID: $issuerDID\n- keyId: $issuanceKeyId ")
//          .toEitherT[Task]
//
//      revokedOn =
//        issuingKeyProto.revokedOn
//          .flatMap(ledgerData => ledgerData.timestampInfo)
//          .map(fromTimestampInfoProto)
//    } yield new credentials.KeyData(issuingKey, addedOn, revokedOn.orNull)
//  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] =
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKeyFromByteCoordinates(maybeX.x.toByteArray, maybeY.y.toByteArray)

  def toTimestampInfoProto(ecPublicKey: ECPublicKey): node_models.ECKeyData = {
    val point = ecPublicKey.getCurvePoint

    node_models.ECKeyData(
      ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

//  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): protos.TimestampInfo = {
//    import scala.jdk.CollectionConverters._
//
//    val time = timestampInfoProto.blockTimestamp.get
//    val timestamp = new pbandk.wkt.Timestamp(time.seconds, time.nanos, Map().asJava)
//
//    new protos.TimestampInfo(
//      timestampInfoProto.blockSequenceNumber,
//      timestampInfoProto.operationSequenceNumber,
//      timestampInfoProto.blockTimestamp.map(_.),
//      Map().asJava
//    )
//  }

  def toInfoProto(timestampInfoProto: TimestampInfo): node_models.TimestampInfo = {
    node_models.TimestampInfo(
      blockSequenceNumber = timestampInfoProto.blockSequenceNumber,
      operationSequenceNumber = timestampInfoProto.operationSequenceNumber,
      blockTimestamp = timestampInfoProto.getBlockTimestamp.some
    )
  }

  def issueBatchOperation(issuerDID: PrismDid, merkleRoot: MerkleRoot): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
          value = node_models
            .IssueCredentialBatchOperation(
              credentialBatchData = Some(
                node_models.CredentialBatchData(
                  issuerDid = issuerDID.getSuffix,
                  merkleRoot = toByteString(merkleRoot.getHash)
                )
              )
            )
        )
      )
  }

  def revokeCredentialsOperation(
      previousOperationHash: Sha256Digest,
      batchId: CredentialBatchId,
      credentialsToRevoke: Seq[Sha256Digest] = Nil
  ): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.RevokeCredentials(
          value = node_models
            .RevokeCredentialsOperation(
              previousOperationHash = toByteString(previousOperationHash),
              credentialBatchId = batchId.getId,
              credentialsToRevoke = credentialsToRevoke.map(toByteString)
            )
        )
      )
  }

  def toByteString(hash: Sha256Digest): ByteString = ByteString.copyFrom(hash.getValue)

  def toSha256Digest(byteString: ByteString): Sha256Digest =
    Sha256Digest.fromBytes(byteString.toByteArray)
}
