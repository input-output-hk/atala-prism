package io.iohk.atala.prism.management.console.grpc

import cats.syntax.option._
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService.DetailedContactWithConnection
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential, InstitutionGroup, Statistics, _}
import io.iohk.atala.prism.protos.common_models.OperationStatus.UNKNOWN_OPERATION
import io.iohk.atala.prism.protos.console_api.GetContactResponse
import io.iohk.atala.prism.protos.console_models.{ContactConnectionStatus, Group, StoredSignedCredential}
import io.iohk.atala.prism.protos.{common_models, connector_models, console_api, console_models}
import io.iohk.atala.prism.utils.syntax._
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

import java.time.{Instant, LocalDate}
import io.iohk.atala.prism.protos.connector_models.ContactConnection

import scala.util.{Failure, Success, Try}

object ProtoCodecs {

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(
      year = date.getYear,
      month = date.getMonthValue,
      day = date.getDayOfMonth
    )
  }

  implicit val credentialTypeStateProtoTransformer
      : Transformer[CredentialTypeState, console_models.CredentialTypeState] = {
    case CredentialTypeState.Archived =>
      console_models.CredentialTypeState.CREDENTIAL_TYPE_ARCHIVED
    case CredentialTypeState.Draft =>
      console_models.CredentialTypeState.CREDENTIAL_TYPE_DRAFT
    case CredentialTypeState.Ready =>
      console_models.CredentialTypeState.CREDENTIAL_TYPE_READY
  }

  implicit val credentialTypeFieldTypeProtoTransformer: Transformer[
    CredentialTypeFieldType,
    console_models.CredentialTypeFieldType
  ] = {
    case CredentialTypeFieldType.String =>
      console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_STRING
    case CredentialTypeFieldType.Int =>
      console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_INT
    case CredentialTypeFieldType.Boolean =>
      console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_BOOLEAN
    case CredentialTypeFieldType.Date =>
      console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_DATE
  }

  def toGetContactResponse(
      maybeDetailedContactWithConnection: Option[DetailedContactWithConnection]
  ): GetContactResponse = {
    val contactWithDetails =
      maybeDetailedContactWithConnection.map(_.contactWithDetails)
    console_api.GetContactResponse(
      contact = maybeDetailedContactWithConnection.map(detailedContactWithConnection =>
        toContactProto(
          detailedContactWithConnection.contactWithDetails.contact,
          detailedContactWithConnection.connection
        )
      ),
      groups = contactWithDetails
        .map(_.groupsInvolved)
        .getOrElse(List.empty)
        .map(groupWithContactCountToProto),
      receivedCredentials = contactWithDetails
        .map(_.receivedCredentials)
        .getOrElse(List.empty)
        .map(receivedSignedCredentialToProto),
      issuedCredentials = maybeDetailedContactWithConnection
        .map { detailedContactWithConnection =>
          val issuedCredentials =
            detailedContactWithConnection.contactWithDetails.issuedCredentials
          val connections =
            detailedContactWithConnection.issuedCredentialsConnections
          issuedCredentials.map(issuedCredential =>
            genericCredentialToProto(
              issuedCredential,
              connections.getOrElse(
                issuedCredential.connectionToken,
                ContactConnection(
                  connectionToken = issuedCredential.connectionToken.token,
                  connectionStatus = ContactConnectionStatus.STATUS_CONNECTION_MISSING
                )
              )
            )
          )
        }
        .toList
        .flatten
    )
  }

  def groupWithContactCountToProto(
      group: InstitutionGroup.WithContactCount
  ): Group = {
    console_models
      .Group()
      .withId(group.value.id.toString)
      .withCreatedAt(group.value.createdAt.toProtoTimestamp)
      .withName(group.value.name.value)
      .withNumberOfContacts(group.numberOfContacts)
  }

  def receivedSignedCredentialToProto(
      receivedSignedCredential: ReceivedSignedCredential
  ): StoredSignedCredential = {
    console_models.StoredSignedCredential(
      individualId = receivedSignedCredential.individualId.toString,
      encodedSignedCredential = receivedSignedCredential.encodedSignedCredential,
      storedAt = receivedSignedCredential.receivedAt.toProtoTimestamp.some
    )
  }

  def genericCredentialToProto(
      credential: GenericCredential,
      connection: connector_models.ContactConnection
  ): console_models.CManagerGenericCredential = {
    val revokedOnOperationId = credential.revokedOnOperationId
      .map(_.value)
      .getOrElse(Vector.empty)

    val model = console_models
      .CManagerGenericCredential()
      .withCredentialId(credential.credentialId.toString)
      .withIssuerId(credential.issuedBy.uuid.toString)
      .withContactId(credential.contactId.toString)
      .withCredentialData(credential.credentialData.noSpaces)
      .withIssuerName(credential.issuerName)
      .withContactData(credential.contactData.noSpaces)
      .withConnectionStatus(connection.connectionStatus)
      .withExternalId(credential.externalId.value)
      .withRevokedOnOperationStatus(toOperationStatus(credential.revokedOnOperationStatus))
      .withSharedAt(
        credential.sharedAt.map(_.toProtoTimestamp).getOrElse(Timestamp())
      )
      .withRevokedOnOperationId(ByteString.copyFrom(revokedOnOperationId.toArray))
    credential.publicationData.fold(model) { data =>
      model
        .withBatchId(data.credentialBatchId.getId)
        .withIssuanceOperationHash(
          ByteString.copyFrom(data.issuanceOperationHash.getValue)
        )
        .withEncodedSignedCredential(data.encodedSignedCredential)
        .withBatchInclusionProof(data.inclusionProof.encode)
        .withPublicationStoredAt(data.storedAt.toProtoTimestamp)
    }
  }

  def toOperationStatus(
      operationStatus: Option[OperationStatus]
  ): common_models.OperationStatus = {
    operationStatus.flatMap(os => common_models.OperationStatus.fromName(os.entryName)).getOrElse(UNKNOWN_OPERATION)
  }

  def toContactProto(
      contact: Contact,
      connection: connector_models.ContactConnection
  ): console_models.Contact = {
    console_models
      .Contact()
      .withContactId(contact.contactId.toString)
      .withExternalId(contact.externalId.value)
      .withJsonData(contact.data.noSpaces)
      .withConnectionStatus(connection.connectionStatus)
      .withConnectionToken(connection.connectionToken)
      .withConnectionId(connection.connectionId)
      .withName(contact.name)
      .withCreatedAt(contact.createdAt.toProtoTimestamp)
  }

  def toStatisticsProto(
      statistics: Statistics
  ): console_api.GetStatisticsResponse = {
    statistics
      .into[console_api.GetStatisticsResponse]
      .withFieldConst(
        _.numberOfCredentialsInDraft,
        statistics.numberOfCredentialsInDraft
      )
      .transform
  }

  def toCredentialTypeFieldProto(
      credentialTypeField: CredentialTypeField
  ): console_models.CredentialTypeField = {
    credentialTypeField
      .into[console_models.CredentialTypeField]
      .withFieldConst(
        _.credentialTypeId,
        credentialTypeField.credentialTypeId.uuid.toString
      )
      .withFieldConst(_.id, credentialTypeField.id.uuid.toString)
      .transform
  }

  def toCredentialTypeProto(
      credentialType: CredentialType
  ): console_models.CredentialType = {
    credentialType
      .into[console_models.CredentialType]
      .withFieldConst(
        _.createdAt,
        credentialType.createdAt.toProtoTimestamp.some
      )
      .withFieldConst(_.id, credentialType.id.uuid.toString)
      .withFieldConst(
        _.icon,
        credentialType.icon
          .map(icon => ByteString.copyFrom(icon.toArray))
          .getOrElse(ByteString.EMPTY)
      )
      .transform
  }

  def toCredentialTypeCategoryProto(
      credentialTypeCategory: CredentialTypeCategory
  ): console_models.CredentialTypeCategory = {
    credentialTypeCategory
      .into[console_models.CredentialTypeCategory]
      .withFieldComputed(_.id, _.id.uuid.toString)
      .withFieldComputed(_.institutionId, _.institutionId.uuid.toString)
      .withFieldComputed(
        _.state,
        _.state match {
          case CredentialTypeCategoryState.Draft =>
            console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_DRAFT
          case CredentialTypeCategoryState.Ready =>
            console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_READY
          case CredentialTypeCategoryState.Archived =>
            console_models.CredentialTypeCategoryState.CREDENTIAL_TYPE_CATEGORY_ARCHIVED
        }
      )
      .transform
  }

  def toCredentialTypeWithRequiredFieldsProto(
      withFields: CredentialTypeWithRequiredFields
  ): console_models.CredentialTypeWithRequiredFields = {
    withFields
      .into[console_models.CredentialTypeWithRequiredFields]
      .withFieldConst(
        _.credentialType,
        Some(toCredentialTypeProto(withFields.credentialType))
      )
      .withFieldConst(
        _.requiredFields,
        withFields.requiredFields.map(toCredentialTypeFieldProto)
      )
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

  def toTimestamp(
      timeInterval: common_models.TimeInterval
  ): Try[TimeInterval] = {
    for {
      startTimestamp <- readTimestamp(
        timeInterval.startTimestamp,
        "Starting timestamp was not specified"
      )
      endTimestamp <- readTimestamp(
        timeInterval.endTimestamp,
        "Ending timestamp was not specified"
      )
      _ <-
        if (startTimestamp.isAfter(endTimestamp))
          Failure(
            new IllegalArgumentException(
              "Starting timestamp cannot be after the ending timestamp"
            )
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
      case Timestamp(0L, _, _) =>
        Failure(new IllegalArgumentException(messageOnError))
      case timestamp => Success(timestamp.toInstant)
    }
}
