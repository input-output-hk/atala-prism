package io.iohk.atala.prism.management.console.grpc

import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential}
import io.iohk.atala.prism.protos.{cmanager_models, common_models, console_models}
import io.scalaland.chimney.Transformer

object ProtoCodecs {

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  def genericCredentialToProto(credential: GenericCredential): cmanager_models.CManagerGenericCredential = {
    val model = cmanager_models
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

  def toContactProto(contact: Contact): console_models.Contact = {
    console_models
      .Contact()
      .withContactId(contact.contactId.value.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withCreatedAt(contact.createdAt.toEpochMilli)
  }
}
