package io.iohk.atala.cvp.webextension.background.services.node

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.iohk.atala.cvp.webextension.background.services.node.NodeUtils._
import io.iohk.atala.prism.credentials.{
  BatchData,
  Credential,
  CredentialBatchId,
  KeyData,
  PrismCredentialVerification,
  TimestampInfo,
  VerificationError
}
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.crypto.{EC, ECTrait, SHA256Digest}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.{
  GetBatchStateRequest,
  GetCredentialRevocationTimeRequest,
  GetDidDocumentRequest
}
import scalapb.grpc.Channels

import scala.concurrent.{ExecutionContext, Future}

class NodeClientService(url: String) {
  private implicit val ec: ECTrait = EC

  private val nodeServiceApi = node_api.NodeServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def verifyCredential(
      signedCredentialStringRepresentation: String,
      merkleProof: MerkleInclusionProof
  )(implicit executionContext: ExecutionContext): Future[ValidatedNel[VerificationError, Unit]] = {

    for {
      credential <-
        Credential
          .fromString(signedCredentialStringRepresentation)
          .fold(
            error => Future.failed(new RuntimeException(error.message)),
            Future.successful
          )

      issuerDid <-
        credential.content.issuerDid
          .map(Future.successful)
          .getOrElse(Future.failed(new Throwable(s"Cannot verify credential: $credential, issuerDID is empty")))

      issuanceKeyId <-
        credential.content.issuanceKeyId
          .map(Future.successful)
          .getOrElse(Future.failed(new Throwable(s"Cannot verify credential: $credential, issuanceKeyId is empty")))

      keyData <- getKeyData(issuerDID = issuerDid, issuanceKeyId = issuanceKeyId)
      batchId = CredentialBatchId.fromBatchData(issuerDid.suffix, merkleProof.derivedRoot)
      batchData <- getBatchData(batchId)
      credentialRevocationTime <- getCredentialRevocationTime(batchId, credential.hash)
    } yield PrismCredentialVerification.verify(
      keyData,
      batchData,
      credentialRevocationTime,
      merkleProof.derivedRoot,
      merkleProof,
      credential
    )
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
    } yield KeyData(publicKey = issuingKey, addedOn = addedOn, revokedOn = revokedOn)
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
    } yield BatchData(issuedOn = publishedOn, revokedOn = revokedOn)
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
