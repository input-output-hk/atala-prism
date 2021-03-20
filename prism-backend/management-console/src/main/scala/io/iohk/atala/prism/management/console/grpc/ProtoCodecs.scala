package io.iohk.atala.prism.management.console.grpc

import cats.syntax.option._
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.models.{ProtoCodecs => CommonProtoCodecs}
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService.DetailedContactWithConnection
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential, InstitutionGroup, Statistics, _}
import io.iohk.atala.prism.protos.console_api.GetContactResponse
import io.iohk.atala.prism.protos.console_models.{Group, StoredSignedCredential}
import io.iohk.atala.prism.protos.{common_models, connector_models, console_api, console_models}
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._
import java.time.{Instant, LocalDate}

import io.iohk.atala.prism.models.TransactionInfo

import scala.util.{Failure, Success, Try}

object ProtoCodecs {

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
  }

  implicit val credentialTypeStateProtoTransformer
      : Transformer[CredentialTypeState, console_models.CredentialTypeState] = {
    case CredentialTypeState.Archived => console_models.CredentialTypeState.CREDENTIAL_TYPE_ARCHIVED
    case CredentialTypeState.Draft => console_models.CredentialTypeState.CREDENTIAL_TYPE_DRAFT
    case CredentialTypeState.Ready => console_models.CredentialTypeState.CREDENTIAL_TYPE_READY
  }

  implicit val credentialTypeFieldTypeProtoTransformer
      : Transformer[CredentialTypeFieldType, console_models.CredentialTypeFieldType] = {
    case CredentialTypeFieldType.String => console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_STRING
    case CredentialTypeFieldType.Int => console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_INT
    case CredentialTypeFieldType.Boolean => console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_BOOLEAN
    case CredentialTypeFieldType.Date => console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_DATE
  }

  def toGetContactResponse(detailedContactWithConnection: Option[DetailedContactWithConnection]): GetContactResponse = {
    val contactWithDetails = detailedContactWithConnection.map(_.contactWithDetails)
    console_api.GetContactResponse(
      contact = detailedContactWithConnection.map(detailedContactWithConnection =>
        toContactProto(
          detailedContactWithConnection.contactWithDetails.contact,
          detailedContactWithConnection.connection
        )
      ),
      groups = contactWithDetails.map(_.groupsInvolved).getOrElse(List.empty).map(groupWithContactCountToProto),
      receivedCredentials =
        contactWithDetails.map(_.receivedCredentials).getOrElse(List.empty).map(receivedSignedCredentialToProto),
      issuedCredentials =
        contactWithDetails.map(_.issuedCredentials).getOrElse(List.empty).map(genericCredentialToProto)
    )
  }

  def groupWithContactCountToProto(group: InstitutionGroup.WithContactCount): Group = {
    console_models
      .Group()
      .withId(group.value.id.toString)
      .withCreatedAtDeprecated(group.value.createdAt.getEpochSecond)
      .withCreatedAt(group.value.createdAt.toProtoTimestamp)
      .withName(group.value.name.value)
      .withNumberOfContacts(group.numberOfContacts)
  }

  def receivedSignedCredentialToProto(receivedSignedCredential: ReceivedSignedCredential): StoredSignedCredential = {
    console_models.StoredSignedCredential(
      individualId = receivedSignedCredential.individualId.toString,
      encodedSignedCredential = receivedSignedCredential.encodedSignedCredential,
      storedAtDeprecated = receivedSignedCredential.receivedAt.toEpochMilli,
      storedAt = receivedSignedCredential.receivedAt.toProtoTimestamp.some
    )
  }

  def genericCredentialToProto(credential: GenericCredential): console_models.CManagerGenericCredential = {
    val model = console_models
      .CManagerGenericCredential()
      .withCredentialId(credential.credentialId.toString)
      .withIssuerId(credential.issuedBy.uuid.toString)
      .withContactId(credential.subjectId.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withContactData(credential.subjectData.noSpaces)
      .withExternalId(credential.externalId.value)
      .withSharedAtDeprecated(credential.sharedAt.map(_.toEpochMilli).getOrElse(0))
      .withSharedAt(credential.sharedAt.map(_.toProtoTimestamp).getOrElse(Timestamp()))

    credential.publicationData.fold(model) { data =>
      model
        .withNodeCredentialId("") // deprecated
        .withBatchId(data.credentialBatchId.id)
        .withIssuanceOperationHash(ByteString.copyFrom(data.issuanceOperationHash.value.toArray))
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withBatchInclusionProof(data.inclusionProof.encode)
        .withPublicationStoredAtDeprecated(data.storedAt.toEpochMilli)
        .withPublicationStoredAt(data.storedAt.toProtoTimestamp)
        .withIssuanceProof(CommonProtoCodecs.toTransactionInfo(TransactionInfo(data.transactionId, data.ledger)))
    }
  }

  def toContactProto(contact: Contact, connection: connector_models.ContactConnection): console_models.Contact = {
    console_models
      .Contact()
      .withContactId(contact.contactId.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withConnectionStatus(connection.connectionStatus)
      .withConnectionToken(connection.connectionToken)
      .withConnectionId(connection.connectionId)
      .withCreatedAtDeprecated(contact.createdAt.toEpochMilli)
      .withName(contact.name)
      .withCreatedAt(contact.createdAt.toProtoTimestamp)
  }

  def toStatisticsProto(statistics: Statistics): console_api.GetStatisticsResponse = {
    statistics
      .into[console_api.GetStatisticsResponse]
      .withFieldConst(_.numberOfCredentialsInDraft, statistics.numberOfCredentialsInDraft)
      .transform
  }

  def toCredentialTypeFieldProto(credentialTypeField: CredentialTypeField): console_models.CredentialTypeField = {
    credentialTypeField
      .into[console_models.CredentialTypeField]
      .withFieldConst(_.credentialTypeId, credentialTypeField.credentialTypeId.uuid.toString)
      .withFieldConst(_.id, credentialTypeField.id.uuid.toString)
      .transform
  }

  def toCredentialTypeProto(credentialType: CredentialType): console_models.CredentialType = {
    credentialType
      .into[console_models.CredentialType]
      .withFieldConst(_.createdAt, credentialType.createdAt.toProtoTimestamp.some)
      .withFieldConst(_.id, credentialType.id.uuid.toString)
      .withFieldConst(
        _.icon,
        credentialType.icon.map(icon => ByteString.copyFrom(icon.toArray)).getOrElse(ByteString.EMPTY)
      )
      .transform
  }

  def toCredentialTypeWithRequiredFieldsProto(
      withFields: CredentialTypeWithRequiredFields
  ): console_models.CredentialTypeWithRequiredFields = {
    withFields
      .into[console_models.CredentialTypeWithRequiredFields]
      .withFieldConst(_.credentialType, Some(toCredentialTypeProto(withFields.credentialType)))
      .withFieldConst(_.requiredFields, withFields.requiredFields.map(toCredentialTypeFieldProto))
      .transform
  }

  def checkListUniqueness[T](list: List[T]): Try[Set[T]] = {
    val set = list.toSet
    if (set.size == list.size) {
      Success(set)
    } else {
      Failure(
        new IllegalArgumentException(
          s"List [${list.mkString(", ")}] repeats itself"
        )
      )
    }
  }

  def toTimestamp(timeInterval: common_models.TimeInterval): Try[TimeInterval] = {
    for {
      startTimestamp <- readTimestamp(timeInterval.startTimestamp, "Starting timestamp was not specified")
      endTimestamp <- readTimestamp(timeInterval.endTimestamp, "Ending timestamp was not specified")
      _ <-
        if (startTimestamp.isAfter(endTimestamp))
          Failure(
            new IllegalArgumentException("Starting timestamp cannot be after the ending timestamp")
          )
        else
          Success(())
    } yield TimeInterval(startTimestamp, endTimestamp)
  }

  private def readTimestamp(
      maybeTimestamp: Option[Timestamp],
      messageOnError: String
  ): Try[Instant] =
    maybeTimestamp.fold[Try[Instant]] {
      Failure(new IllegalArgumentException(messageOnError))
    } {
      case Timestamp(0L, _, _) => Failure(new IllegalArgumentException(messageOnError))
      case timestamp => Success(timestamp.toInstant)
    }
}
