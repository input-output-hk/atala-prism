package io.iohk.atala.cvp.webextension.background.services.node

import io.iohk.atala.credentials.{
  CredentialData,
  CredentialVerification,
  CredentialsCryptoSDKImpl,
  JsonBasedUnsignedCredential,
  KeyData,
  SignedCredential
}
import io.iohk.atala.crypto.SHA256Digest
import io.iohk.prism.protos.node_api.{GetCredentialStateRequest, GetDidDocumentRequest}
import io.iohk.prism.protos.node_api
import scalapb.grpc.Channels

import scala.concurrent.{ExecutionContext, Future}
import NodeUtils._

class NodeClientService(url: String) {
  private val nodeServiceApi = node_api.NodeServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def verifyCredential(
      signedCredentialStringRepresentation: String
  )(implicit executionContext: ExecutionContext): Future[Boolean] = {

    for {
      signedCredential <- Future.fromTry(SignedCredential.from(signedCredentialStringRepresentation))
      unsignedCredential = signedCredential.decompose[JsonBasedUnsignedCredential].credential
      issuerDID = unsignedCredential.issuerDID.get
      issuanceKeyId = unsignedCredential.issuanceKeyId.get
      keyData <- getKeyData(issuerDID = issuerDID, issuanceKeyId = issuanceKeyId)
      credentialData <- getCredentialData(
        computeNodeCredentialId(
          credentialHash = CredentialsCryptoSDKImpl.hash(signedCredential),
          didSuffix = issuerDID.stripPrefix("did:prism:")
        )
      )
    } yield CredentialVerification.verifyCredential(keyData, credentialData, signedCredential)
  }

  private def getKeyData(issuerDID: String, issuanceKeyId: String)(implicit ec: ExecutionContext): Future[KeyData] = {
    for {
      response <- nodeServiceApi.getDidDocument(GetDidDocumentRequest().withDid(issuerDID))
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

  private def getCredentialData(nodeCredentialId: String)(implicit ec: ExecutionContext): Future[CredentialData] = {
    for {
      response <- nodeServiceApi.getCredentialState(
        GetCredentialStateRequest().withCredentialId(nodeCredentialId)
      )
      publishedOn =
        response.publicationDate
          .map(fromTimestampInfoProto)
          .getOrElse(throw new Exception(s"Missing publication date $nodeCredentialId"))
      revokedOn = response.revocationDate map fromTimestampInfoProto
    } yield CredentialData(issuedOn = publishedOn, revokedOn = revokedOn)
  }
}

object NodeClientService {
  def apply(url: String): NodeClientService = new NodeClientService(url)
}
