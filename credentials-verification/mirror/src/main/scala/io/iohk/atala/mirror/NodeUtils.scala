package io.iohk.atala.mirror

import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.atala.prism.credentials
import io.iohk.atala.prism.crypto.{EC, ECConfig, ECPublicKey, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.AtalaOperation
import cats.data.EitherT
import io.iohk.atala.mirror.services.NodeClientService
import io.iohk.atala.prism.credentials.KeyData
import monix.eval.Task
import cats.implicits._

object NodeUtils {

  def getKeyData(
      issuerDID: DID,
      issuanceKeyId: String,
      nodeService: NodeClientService
  ): EitherT[Task, String, KeyData] = {
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
    } yield KeyData(publicKey = issuingKey, addedOn = addedOn, revokedOn = revokedOn)
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
                issuer = did.stripPrismPrefix,
                contentHash = ByteString.copyFrom(credentialHash.value.toArray)
              )
            )
          )
        )
      )
  }
}
