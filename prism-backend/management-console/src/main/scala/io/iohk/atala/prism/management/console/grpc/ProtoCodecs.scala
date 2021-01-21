package io.iohk.atala.prism.management.console.grpc

import com.google.protobuf.ByteString
import io.iohk.atala.prism.management.console.models.{Contact, CredentialIssuance, GenericCredential, Statistics}
import io.iohk.atala.prism.protos.{common_models, connector_models, console_api, console_models}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

import java.time.LocalDate

object ProtoCodecs {

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  def genericCredentialToProto(credential: GenericCredential): console_models.CManagerGenericCredential = {
    val model = console_models
      .CManagerGenericCredential()
      .withCredentialId(credential.credentialId.value.toString)
      .withIssuerId(credential.issuedBy.uuid.toString)
      .withContactId(credential.subjectId.value.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withContactData(credential.subjectData.noSpaces)
      .withExternalId(credential.externalId.value)
      .withSharedAt(credential.sharedAt.map(_.toEpochMilli).getOrElse(0))

    credential.publicationData.fold(model) { data =>
      model
        .withNodeCredentialId(data.nodeCredentialId)
        .withIssuanceOperationHash(ByteString.copyFrom(data.issuanceOperationHash.value.toArray))
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withPublicationStoredAt(data.storedAt.toEpochMilli)
    }
  }

  def toContactProto(contact: Contact, connection: connector_models.ContactConnection): console_models.Contact = {
    console_models
      .Contact()
      .withContactId(contact.contactId.value.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withCreatedAt(contact.createdAt.toEpochMilli)
      .withConnectionId(connection.connectionId)
      .withConnectionToken(connection.connectionToken)
      .withConnectionStatus(connection.connectionStatus)
  }

  def toStatisticsProto(statistics: Statistics): console_api.GetStatisticsResponse = {
    statistics
      .into[console_api.GetStatisticsResponse]
      .withFieldConst(_.numberOfCredentialsInDraft, statistics.numberOfCredentialsInDraft)
      .transform
  }

  def toCredentialIssuanceStatusProto(status: CredentialIssuance.Status): console_models.CredentialIssuanceStatus = {
    status match {
      case CredentialIssuance.Status.Draft => console_models.CredentialIssuanceStatus.DRAFT
      case CredentialIssuance.Status.Ready => console_models.CredentialIssuanceStatus.READY
      case CredentialIssuance.Status.Completed => console_models.CredentialIssuanceStatus.COMPLETED
    }
  }
}
