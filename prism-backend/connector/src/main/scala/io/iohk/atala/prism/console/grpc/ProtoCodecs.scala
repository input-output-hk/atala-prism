package io.iohk.atala.prism.console.grpc

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.console.models
import io.iohk.atala.prism.console.models.GenericCredential
import io.iohk.atala.prism.models.{TransactionInfo, ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.protos.{common_models, console_models}
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.Transformer

import java.time.LocalDate

object ProtoCodecs {

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  implicit val contactConnectionStatus2Proto: Transformer[ConnectionStatus, console_models.ContactConnectionStatus] = {
    case ConnectionStatus.InvitationMissing => console_models.ContactConnectionStatus.STATUS_INVITATION_MISSING
    case ConnectionStatus.ConnectionMissing => console_models.ContactConnectionStatus.STATUS_CONNECTION_MISSING
    case ConnectionStatus.ConnectionAccepted => console_models.ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED
    case ConnectionStatus.ConnectionRevoked => console_models.ContactConnectionStatus.STATUS_CONNECTION_REVOKED
  }

  def genericCredentialToProto(credential: GenericCredential): console_models.CManagerGenericCredential = {
    val connectionStatus = contactConnectionStatus2Proto.transform(credential.connectionStatus)
    val revocationProofMaybe = for {
      revocationTxid <- credential.revokedOnTransactionId
      publicationData <- credential.publicationData
    } yield CommonProtoCodecs.toTransactionInfo(TransactionInfo(revocationTxid, publicationData.ledger))

    val model = console_models
      .CManagerGenericCredential(revocationProof = revocationProofMaybe)
      .withCredentialId(credential.credentialId.toString)
      .withIssuerId(credential.issuedBy.toString)
      .withContactId(credential.subjectId.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withGroupName(credential.groupName)
      .withContactData(credential.subjectData.noSpaces)
      .withExternalId(credential.externalId.value)
      .withConnectionStatus(connectionStatus)
      .withSharedAt(credential.sharedAt.map(_.toProtoTimestamp).getOrElse(Timestamp()))

    credential.publicationData.fold(model) { data =>
      model
        .withIssuanceOperationHash(ByteString.copyFrom(data.issuanceOperationHash.value.toArray))
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withPublicationStoredAt(data.storedAt.toProtoTimestamp)
        .withIssuanceProof(CommonProtoCodecs.toTransactionInfo(TransactionInfo(data.transactionId, data.ledger)))
        .withBatchInclusionProof(data.inclusionProof.encode)
        .withBatchId(data.credentialBatchId.id)
    }
  }

  def toContactProto(contact: models.Contact): console_models.Contact = {
    val token = contact.connectionToken.fold("")(_.token)
    val connectionId = contact.connectionId.fold("")(_.toString)
    val status = contactConnectionStatus2Proto.transform(contact.connectionStatus)

    console_models
      .Contact()
      .withContactId(contact.contactId.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withConnectionStatus(status)
      .withConnectionToken(token)
      .withConnectionId(connectionId)
      .withCreatedAt(contact.createdAt.toProtoTimestamp)
  }
}
