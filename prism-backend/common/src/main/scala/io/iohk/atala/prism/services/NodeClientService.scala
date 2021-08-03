package io.iohk.atala.prism.services

import cats.data.EitherT
import monix.eval.Task
import com.google.protobuf.ByteString
import io.iohk.atala.prism.kotlin.crypto.{EC, SHA256Digest}
import io.iohk.atala.prism.kotlin.crypto.keys.ECPublicKey
import io.iohk.atala.prism.kotlin.crypto.ECConfig.{INSTANCE => ECConfig}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.credentials
import io.iohk.atala.prism.protos.{node_api, node_models}
import cats.implicits._
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.crypto.MerkleRoot
import io.iohk.atala.prism.utils.syntax._

trait NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]]

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]]

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse]

  def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: SHA256Digest
  ): Task[GetCredentialRevocationTimeResponse]

}

class NodeClientServiceImpl(node: NodeServiceGrpc.NodeServiceStub, authConfig: DidBasedAuthConfig)
    extends NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]] =
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
          ByteString.copyFrom(EC.sign(operation.toByteArray, authConfig.didIssuingKeyPair.getPrivateKey).getData)
      )

    Task.fromFuture(node.issueCredentialBatch(IssueCredentialBatchRequest().withSignedOperation(signedAtalaOperation)))
  }

  def getCredentialRevocationTime(
      credentialBatchId: CredentialBatchId,
      credentialHash: SHA256Digest
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

  def getKeyData(
      issuerDID: DID,
      issuanceKeyId: String,
      nodeService: NodeClientService
  ): EitherT[Task, String, credentials.KeyData] = {
    for {
      didData <- EitherT(
        nodeService.getDidDocument(issuerDID).map(_.toRight(s"DID Data not found for DID ${issuerDID.getValue}"))
      )

      issuingKeyProto <-
        didData.publicKeys
          .find(_.id == issuanceKeyId)
          .toRight(s"KeyId not found: $issuanceKeyId")
          .toEitherT[Task]

      issuingKey <- fromProtoKey(issuingKeyProto)
        .toRight(s"Failed to parse proto key: $issuingKeyProto")
        .toEitherT[Task]

      addedOn <-
        issuingKeyProto.addedOn
          .map(fromTimestampInfoProto)
          .toRight(s"Missing addedOn time:\n-Issuer DID: $issuerDID\n- keyId: $issuanceKeyId ")
          .toEitherT[Task]

      revokedOn = issuingKeyProto.revokedOn.map(fromTimestampInfoProto)
    } yield new credentials.KeyData(issuingKey, addedOn, revokedOn.orNull)
  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] =
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)

  def toTimestampInfoProto(ecPublicKey: ECPublicKey): node_models.ECKeyData = {
    val point = ecPublicKey.getCurvePoint

    node_models.ECKeyData(
      ECConfig.getCURVE_NAME,
      x = ByteString.copyFrom(point.getX.bytes()),
      y = ByteString.copyFrom(point.getY.bytes())
    )
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    new credentials.TimestampInfo(
      timestampInfoProto.blockTimestamp
        .getOrElse(throw new RuntimeException("Missing timestamp"))
        .toInstant
        .toEpochMilli,
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def toInfoProto(timestampInfoProto: credentials.TimestampInfo): node_models.TimestampInfo = {
    node_models.TimestampInfo(
      blockSequenceNumber = timestampInfoProto.getAtalaBlockSequenceNumber,
      operationSequenceNumber = timestampInfoProto.getOperationSequenceNumber,
      blockTimestamp = Timestamp(timestampInfoProto.getAtalaBlockTimestamp).some
    )
  }

  def issueBatchOperation(issuerDID: DID, merkleRoot: MerkleRoot): node_models.AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.IssueCredentialBatch(
          value = node_models
            .IssueCredentialBatchOperation(
              credentialBatchData = Some(
                node_models.CredentialBatchData(
                  issuerDid = issuerDID.getSuffix.getValue,
                  merkleRoot = toByteString(merkleRoot.getHash)
                )
              )
            )
        )
      )
  }

  def revokeCredentialsOperation(
      previousOperationHash: SHA256Digest,
      batchId: CredentialBatchId,
      credentialsToRevoke: Seq[SHA256Digest] = Nil
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

  def toByteString(hash: SHA256Digest): ByteString = ByteString.copyFrom(hash.getValue)

  def toSHA256Digest(byteString: ByteString): SHA256Digest =
    SHA256Digest.fromBytes(byteString.toByteArray)
}
