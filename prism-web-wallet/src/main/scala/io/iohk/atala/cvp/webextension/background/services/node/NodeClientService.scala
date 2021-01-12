package io.iohk.atala.cvp.webextension.background.services.node

import cats.data.ValidatedNel
import io.iohk.atala.cvp.webextension.background.services.node.NodeUtils._
import io.iohk.atala.prism.credentials.{
  Credential,
  CredentialData,
  KeyData,
  PrismCredentialVerification,
  SlayerCredentialId,
  VerificationError
}
import io.iohk.atala.prism.crypto.{EC, ECTrait}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.node_api
import io.iohk.atala.prism.protos.node_api.{GetCredentialStateRequest, GetDidDocumentRequest}
import scalapb.grpc.Channels

import scala.concurrent.{ExecutionContext, Future}

class NodeClientService(url: String) {
  private implicit val ec: ECTrait = EC

  private val nodeServiceApi = node_api.NodeServiceGrpcWeb.stub(Channels.grpcwebChannel(url))

  def verifyCredential(
      signedCredentialStringRepresentation: String
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
      credentialData <- getCredentialData(SlayerCredentialId.compute(credential.hash, issuerDid))
    } yield PrismCredentialVerification.verify(keyData, credentialData, credential)
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

  private def getCredentialData(
      credentialId: SlayerCredentialId
  )(implicit ec: ExecutionContext): Future[CredentialData] = {
    for {
      response <- nodeServiceApi.getCredentialState(
        GetCredentialStateRequest().withCredentialId(credentialId.string)
      )
      publishedOn =
        response.publicationDate
          .map(fromTimestampInfoProto)
          .getOrElse(throw new Exception(s"Missing publication date $credentialId"))
      revokedOn = response.revocationDate map fromTimestampInfoProto
    } yield CredentialData(issuedOn = publishedOn, revokedOn = revokedOn)
  }
}

object NodeClientService {
  def apply(url: String): NodeClientService = new NodeClientService(url)
}
