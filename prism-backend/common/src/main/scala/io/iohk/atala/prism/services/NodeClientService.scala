package io.iohk.atala.prism.services

import java.time.Instant

import cats.data.EitherT
import monix.eval.Task
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder

import io.iohk.atala.prism.config.NodeConfig
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.protos.node_api._
import io.iohk.atala.prism.protos.node_models._
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.protos.node_models

import cats.implicits._

trait NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]]

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse]

  def issueCredential(content: String): Task[IssueCredentialResponse]

}

class NodeClientServiceImpl(node: NodeServiceGrpc.NodeServiceStub, authConfig: DidBasedAuthConfig)
    extends NodeClientService {

  def getDidDocument(did: DID): Task[Option[DIDData]] =
    Task.fromFuture(node.getDidDocument(GetDidDocumentRequest(did.value))).map(_.document)

  def getCredentialState(credentialId: String): Task[GetCredentialStateResponse] =
    Task.fromFuture(node.getCredentialState(GetCredentialStateRequest(credentialId)))

  def issueCredential(content: String): Task[IssueCredentialResponse] = {
    val operation =
      NodeClientService.issueCredentialOperation(SHA256Digest.compute(content.getBytes), authConfig.did)

    val signedAtalaOperation =
      SignedAtalaOperation(
        signedWith = authConfig.didKeyId,
        operation = Some(operation),
        signature = ByteString.copyFrom(EC.sign(operation.toByteArray, authConfig.didKeyPair.privateKey).data)
      )

    Task.fromFuture(node.issueCredential(IssueCredentialRequest().withSignedOperation(signedAtalaOperation)))
  }

}
object NodeClientService {

  /**
    * Create a node gRPC service stub.
    */
  def createNode(
      nodeConfig: NodeConfig
  ): NodeServiceGrpc.NodeServiceStub = {
    val channel = ManagedChannelBuilder
      .forAddress(nodeConfig.host, nodeConfig.port)
      .usePlaintext()
      .build()

    NodeServiceGrpc.stub(channel)
  }

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

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def toInfoProto(timestampInfoProto: credentials.TimestampInfo): node_models.TimestampInfo = {
    node_models.TimestampInfo(
      timestampInfoProto.atalaBlockTimestamp.toEpochMilli,
      timestampInfoProto.atalaBlockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def computeNodeCredentialId(
      credentialHash: SHA256Digest,
      did: DID
  ): String = {
    SHA256Digest
      .compute(
        issueCredentialOperation(credentialHash, did).toByteArray
      )
      .hexValue
  }

  def issueCredentialOperation(
      credentialHash: SHA256Digest,
      did: DID
  ): AtalaOperation = {
    node_models
      .AtalaOperation(
        operation = node_models.AtalaOperation.Operation.IssueCredential(
          node_models.IssueCredentialOperation(
            credentialData = Some(
              node_models.CredentialData(
                issuer = did.suffix.value,
                contentHash = ByteString.copyFrom(credentialHash.value.toArray)
              )
            )
          )
        )
      )
  }
}
