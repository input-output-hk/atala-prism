package io.iohk.atala.cvp.webextension.background.services.node

import java.time.Instant

import com.google.protobuf.ByteString
import io.iohk.atala.credentials
import io.iohk.atala.crypto.{EC, ECPublicKey, SHA256Digest}
import io.iohk.prism.protos.node_models

object NodeUtils {
  def fromProtoKey(protoKey: node_models.PublicKey): Option[ECPublicKey] = {
    for {
      maybeX <- protoKey.keyData.ecKeyData
      maybeY <- protoKey.keyData.ecKeyData
    } yield EC.toPublicKey(maybeX.x.toByteArray, maybeY.y.toByteArray)
  }

  def fromTimestampInfoProto(timestampInfoProto: node_models.TimestampInfo): credentials.TimestampInfo = {
    credentials.TimestampInfo(
      Instant.ofEpochMilli(timestampInfoProto.blockTimestamp),
      timestampInfoProto.blockSequenceNumber,
      timestampInfoProto.operationSequenceNumber
    )
  }

  def computeNodeCredentialId(
      credentialHash: SHA256Digest,
      didSuffix: String
  ): String = {
    SHA256Digest
      .compute(
        node_models
          .AtalaOperation(
            operation = node_models.AtalaOperation.Operation.IssueCredential(
              node_models.IssueCredentialOperation(
                credentialData = Some(
                  node_models.CredentialData(
                    issuer = didSuffix,
                    contentHash = ByteString.copyFrom(credentialHash.value.toArray)
                  )
                )
              )
            )
          )
          .toByteArray
      )
      .hexValue
  }
}
