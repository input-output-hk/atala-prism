package io.iohk.atala.prism.console.grpc

import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.atala.prism.cmanager.models.Student
import io.iohk.atala.prism.console.models
import io.iohk.atala.prism.console.models.Contact.ConnectionStatus
import io.iohk.atala.prism.console.models.GenericCredential
import io.iohk.atala.prism.protos.{cmanager_models, common_models, console_models}
import io.scalaland.chimney.Transformer

object ProtoCodecs {

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  implicit val studentConnectionStatus2Proto
      : Transformer[Student.ConnectionStatus, cmanager_models.StudentConnectionStatus] = {
    case Student.ConnectionStatus.InvitationMissing => cmanager_models.StudentConnectionStatus.InvitationMissing
    case Student.ConnectionStatus.ConnectionMissing => cmanager_models.StudentConnectionStatus.ConnectionMissing
    case Student.ConnectionStatus.ConnectionAccepted => cmanager_models.StudentConnectionStatus.ConnectionAccepted
    case Student.ConnectionStatus.ConnectionRevoked => cmanager_models.StudentConnectionStatus.ConnectionRevoked
  }

  def genericCredentialToProto(credential: GenericCredential): cmanager_models.CManagerGenericCredential = {
    val connectionStatus = studentConnectionStatus2Proto.transform(credential.connectionStatus)

    val model = cmanager_models
      .CManagerGenericCredential()
      .withCredentialId(credential.credentialId.value.toString)
      .withIssuerId(credential.issuedBy.value.toString)
      .withContactId(credential.subjectId.value.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withGroupName(credential.groupName)
      .withContactData(credential.subjectData.noSpaces)
      .withExternalId(credential.externalId.value)
      .withConnectionStatus(connectionStatus)

    credential.publicationData.fold(model) { data =>
      model
        .withNodeCredentialId(data.nodeCredentialId)
        .withIssuanceOperationHash(ByteString.copyFrom(data.issuanceOperationHash.value.toArray))
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withPublicationStoredAt(data.storedAt.toEpochMilli)
    }
  }

  def toContactProto(contact: models.Contact): console_models.Contact = {
    val token = contact.connectionToken.fold("")(_.token)
    val connectionId = contact.connectionId.fold("")(_.id.toString)
    val status = contact.connectionStatus match {
      case ConnectionStatus.InvitationMissing => console_models.ContactConnectionStatus.INVITATION_MISSING
      case ConnectionStatus.ConnectionMissing => console_models.ContactConnectionStatus.CONNECTION_MISSING
      case ConnectionStatus.ConnectionAccepted => console_models.ContactConnectionStatus.CONNECTION_ACCEPTED
      case ConnectionStatus.ConnectionRevoked => console_models.ContactConnectionStatus.CONNECTION_REVOKED
    }

    console_models
      .Contact()
      .withContactId(contact.contactId.value.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withConnectionStatus(status)
      .withConnectionToken(token)
      .withConnectionId(connectionId)
      .withCreatedAt(contact.createdAt.toEpochMilli)
  }
}
