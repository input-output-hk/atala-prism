package io.iohk.atala.prism.services

import java.time.Instant
import cats.data.EitherT
import monix.eval.Task
import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.protos.{node_api, node_models}
import cats.implicits._
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleTree.MerkleRoot
import io.iohk.atala.prism.utils.syntax._

import scala.annotation.nowarn

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
    Task.fromFuture(node.getDidDocument(GetDidDocumentRequest(did.value))).map(_.document)

  def getBatchState(credentialBatchId: CredentialBatchId): Task[Option[GetBatchStateResponse]] =
    Task
      .fromFuture(node.getBatchState(GetBatchStateRequest(credentialBatchId.id)))
      .map { response =>
        if (response.issuerDID.nonEmpty) Some(response)
        else None
      }

  def issueCredentialBatch(merkleRoot: MerkleRoot): Task[IssueCredentialBatchResponse] = {
    val operation =
      NodeClientService.issueBatchOperation(authConfig.did, merkleRoot)

    val signedAtalaOperation =
      SignedAtalaOperation(
        signedWith = authConfig.didKeyId,
        operation = Some(operation),
        signature = ByteString.copyFrom(EC.sign(operation.toByteArray, authConfig.didKeyPair.privateKey).data)
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
          .withBatchId(credentialBatchId.id)
          .withCredentialHash(ByteString.copyFrom(credentialHash.value.toArray))
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
        nodeService.getDidDocument(issuerDID).map(_.toRight(s"DID Data not found for DID ${issuerDID.value}"))
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
    } yield credentials.KeyData(publicKey = issuingKey, addedOn = addedOn, revokedOn = revokedOn)
  }

  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }

  def toTimestampInfoProto(ecPublicKey: ECPublicKey): node_models.ECKeyData = {
    val point = ecPublicKey.getCurvePoint

    node_models.ECKeyData(
      ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(point.x.toByteArray),
      y = ByteString.copyFrom(point.y.toByteArray)
    )
  }

  @nowarn("msg=value blockTimestampDeprecated in class TimestampInfo is deprecated")
  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      timestampInfoProto.blockTimestamp
        .fold(Instant.ofEpochMilli(timestampInfoProto.blockTimestampDeprecated))(_.toInstant),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def toInfoProto(timestampInfoProto: credentials.TimestampInfo): node_models.TimestampInfo = {
    node_models.TimestampInfo(
      timestampInfoProto.atalaBlockTimestamp.toEpochMilli,
      timestampInfoProto.atalaBlockSequenceNumber,
      timestampInfoProto.operationSequenceNumber,
      timestampInfoProto.atalaBlockTimestamp.toProtoTimestamp.some
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
                  issuerDID = issuerDID.suffix.value,
                  merkleRoot = toByteString(merkleRoot.hash)
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
              credentialBatchId = batchId.id,
              credentialsToRevoke = credentialsToRevoke.map(toByteString)
            )
        )
      )
  }

  def toByteString(hash: SHA256Digest): ByteString = ByteString.copyFrom(hash.value.toArray)

  def toSHA256Digest(byteString: ByteString): SHA256Digest =
    SHA256Digest.fromVectorUnsafe(byteString.toByteArray.toVector)
}
