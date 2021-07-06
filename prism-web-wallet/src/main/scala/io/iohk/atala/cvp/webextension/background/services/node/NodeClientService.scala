package io.iohk.atala.cvp.webextension.background.services.node

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import com.google.protobuf.ByteString
import io.iohk.atala.cvp.webextension.background.services.node.NodeUtils._
import io.iohk.atala.cvp.webextension.util.NullableOps._
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateRequest,
  GetCredentialRevocationTimeRequest,
  GetDidDocumentRequest
}
import scalapb.grpc.Channels
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredentialCompanion
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.credentials.{
  BatchData,
  CredentialBatchId,
  CredentialBatchIdCompanion,
  CredentialVerification,
  KeyData,
  TimestampInfo,
  VerificationException
}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.{MerkleInclusionProof, SHA256Digest}
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.DID

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.JavaScriptException

class NodeClientService(url: String) {
  private val nodeServiceApi = node_api.NodeServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def verifyCredential(
      signedCredentialStringRepresentation: String,
      merkleProof: MerkleInclusionProof
  )(implicit executionContext: ExecutionContext): Future[ValidatedNel[VerificationException, Unit]] = {

    for {
      credential <- Future.successful(
        JsonBasedCredentialCompanion
          .fromString(signedCredentialStringRepresentation)
      )

      issuerDid =
        credential.content
          .getIssuerDid()
          .getNullable(throw new RuntimeException(s"Cannot verify credential: $credential, issuerDID is empty"))

      issuanceKeyId =
        credential.content
          .getIssuanceKeyId()
          .getNullable(throw new RuntimeException(s"Cannot verify credential: $credential, issuanceKeyId is empty"))

      keyData <- getKeyData(issuerDID = issuerDid, issuanceKeyId = issuanceKeyId)
      batchId = CredentialBatchIdCompanion.fromBatchData(issuerDid.suffix, merkleProof.derivedRoot())
      batchData <- getBatchData(batchId)
      credentialRevocationTime <- getCredentialRevocationTime(batchId, credential.hash())
      verificationResult =
        try {
          CredentialVerification.verifyMerkle(
            keyData,
            batchData,
            credentialRevocationTime.orNull[TimestampInfo],
            merkleProof.derivedRoot(),
            merkleProof,
            credential
          )
          Valid(())
        } catch {
          case JavaScriptException(e: VerificationException) => Invalid(NonEmptyList(e, Nil))
        }
    } yield verificationResult
  }

  private def getKeyData(issuerDID: DID, issuanceKeyId: String)(implicit ec: ExecutionContext): Future[KeyData] = {
    for {
      response <- nodeServiceApi.getDidDocument(GetDidDocumentRequest().withDid(issuerDID.value))
      didDocument = response.document getOrElse (throw new Exception(s"DID Data not found for DID $issuerDID"))
      issuingKeyProto = didDocument.publicKeys.find(_.id == issuanceKeyId) getOrElse (throw new Exception(
        s"KeyId not found: $issuanceKeyId"
      ))
      issuingKey =
        fromProtoKey(issuingKeyProto) getOrElse (throw new Exception(s"Failed to parse proto key: $issuingKeyProto"))
      addedOn =
        issuingKeyProto.addedOn
          .map(fromTimestampInfoProto)
          .getOrElse(throw new Exception(s"Missing addedOn time:\n-Issuer DID: $issuerDID\n- keyId: $issuanceKeyId "))
      revokedOn = issuingKeyProto.revokedOn.map(fromTimestampInfoProto)
    } yield new KeyData(
      publicKey = issuingKey,
      addedOn = addedOn,
      revokedOn = revokedOn.orNull[TimestampInfo]
    )
  }

  // TODO: Detect when the batch isn't found in the node, and report a reasonable error instead of
  // reporting missing fields because the caller doesn't know about those
  private def getBatchData(
      batchId: CredentialBatchId
  )(implicit ec: ExecutionContext): Future[BatchData] = {
    for {
      response <- nodeServiceApi.getBatchState(
        GetBatchStateRequest().withBatchId(batchId.id)
      )
      publishedOn =
        response.publicationLedgerData
          .flatMap(_.timestampInfo.map(fromTimestampInfoProto))
          .getOrElse(throw new Exception(s"Missing publication date $batchId"))
      revokedOn = response.revocationLedgerData.flatMap(_.timestampInfo.map(fromTimestampInfoProto))
    } yield new BatchData(issuedOn = publishedOn, revokedOn = revokedOn.orNull[TimestampInfo])
  }

  def getCredentialRevocationTime(
      batchId: CredentialBatchId,
      credentialHash: SHA256Digest
  )(implicit ec: ExecutionContext): Future[Option[TimestampInfo]] = {
    for {
      response <- nodeServiceApi.getCredentialRevocationTime(
        GetCredentialRevocationTimeRequest()
          .withBatchId(batchId.id)
          .withCredentialHash(ByteString.copyFrom(credentialHash.value.toArray))
      )
    } yield response.revocationLedgerData.flatMap(_.timestampInfo.map(fromTimestampInfoProto))
  }
}

object NodeClientService {
  def apply(url: String): NodeClientService = new NodeClientService(url)
}
