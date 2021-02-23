package io.iohk.atala.prism.management.console.grpc

import com.google.protobuf.ByteString
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService.DetailedContactWithConnection
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential, InstitutionGroup, Statistics, _}
import io.iohk.atala.prism.management.console.validations.JsonValidator
import io.iohk.atala.prism.protos.common_models.SortByDirection
import io.iohk.atala.prism.protos.console_api.GetContactResponse
import io.iohk.atala.prism.protos.console_models.{Group, StoredSignedCredential}
import io.iohk.atala.prism.protos.{common_models, connector_models, console_api, console_models}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

import java.time.{Instant, LocalDate}
import scala.util.{Failure, Success, Try}

object ProtoCodecs {

  import PaginatedQueryConstraints._

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
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
      .withCreatedAt(group.value.createdAt.getEpochSecond)
      .withName(group.value.name.value)
      .withNumberOfContacts(group.numberOfContacts)
  }

  def receivedSignedCredentialToProto(receivedSignedCredential: ReceivedSignedCredential): StoredSignedCredential = {
    console_models.StoredSignedCredential(
      individualId = receivedSignedCredential.individualId.toString,
      encodedSignedCredential = receivedSignedCredential.encodedSignedCredential,
      storedAt = receivedSignedCredential.receivedAt.toEpochMilli
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
      .withContactId(contact.contactId.toString)
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

  def toCreateContactBatch(request: console_api.CreateContactsRequest): Try[CreateContact.Batch] = {
    for {
      validatedGroups <- toGroupIdSet(request.groups)
      validatedContacts <- toCreateContacts(request.contacts)
      _ = if (validatedContacts.isEmpty) throw new RuntimeException("There are no contacts to create")
    } yield CreateContact.Batch(validatedGroups, validatedContacts)
  }

  def toCreateContacts(request: Seq[console_api.CreateContactsRequest.Contact]): Try[List[CreateContact.NoOwner]] = {
    val validatedContacts = request.map(toCreateContact).flatMap(_.toOption)
    val externalIdCount = validatedContacts.map(_.externalId).distinct.size
    if (externalIdCount != request.size) {
      Failure(
        new RuntimeException(
          "The contact list is invalid, make sure that all externalId are unique, and the contact format is correct"
        )
      )
    } else {
      Success(validatedContacts.toList)
    }
  }

  def toCreateContact(request: console_api.CreateContactsRequest.Contact): Try[CreateContact.NoOwner] = {
    for {
      json <- JsonValidator.jsonData(request.jsonData)
      externalId <- Contact.ExternalId.validated(request.externalId)
    } yield CreateContact.NoOwner(externalId, json, request.name)
  }

  def toGroupIdSet(request: Seq[String]): Try[Set[InstitutionGroup.Id]] = {
    val validatedGroups = request.map(InstitutionGroup.Id.from).flatMap(_.toOption).toSet
    if (validatedGroups.size != request.size) {
      Failure(
        new RuntimeException(
          "The given group list is invalid, make sure that all ids have the correct format, and there aren't repeated groups"
        )
      )
    } else {
      Success(validatedGroups)
    }
  }

  def toContactsPaginatedQuery(request: console_api.GetContactsRequest): Try[Contact.PaginatedQuery] = {
    val scrollIdT = Contact.Id.optional(request.scrollId)
    val createdAtT = Try {
      request.filterBy
        .flatMap(_.createdBy)
        .map(proto2DateTransformer.transform)
    }

    val name = request.filterBy.map(_.name).map(_.trim).filter(_.nonEmpty)
    val groupName = InstitutionGroup.Name.optional(request.groupName)

    val defaultSortBy = ResultOrdering(Contact.SortBy.createdAt)
    val sortByT = request.sortBy.map(toContactsResultOrdering).getOrElse(Try(defaultSortBy))
    val allowedLimit = 0 to 100
    val defaultLimit = 10
    val limitT = Try {
      if (allowedLimit contains request.limit) request.limit
      else throw new RuntimeException(s"Invalid limit, allowed values are $allowedLimit")
    }.map {
      case 0 => defaultLimit
      case x => x
    }

    for {
      scrollId <- scrollIdT
      createdAt <- createdAtT
      sortBy <- sortByT
      limit <- limitT
    } yield PaginatedQueryConstraints(
      limit = limit,
      ordering = sortBy,
      scrollId = scrollId,
      filters = Some(
        Contact.FilterBy(
          groupName = groupName,
          createdAt = createdAt,
          name = name
        )
      )
    )
  }

  def toContactsResultOrdering(sortBy: console_api.GetContactsRequest.SortBy): Try[ResultOrdering[Contact.SortBy]] = {
    def unsafeField = {
      sortBy.field match {
        case console_api.GetContactsRequest.SortBy.Field.UNKNOWN => Contact.SortBy.createdAt
        case console_api.GetContactsRequest.SortBy.Field.CREATED_AT => Contact.SortBy.createdAt
        case console_api.GetContactsRequest.SortBy.Field.NAME => Contact.SortBy.name
        case console_api.GetContactsRequest.SortBy.Field.EXTERNAL_ID => Contact.SortBy.externalId
        case console_api.GetContactsRequest.SortBy.Field.Unrecognized(x) =>
          throw new RuntimeException(s"Unrecognized SortBy Field: $x")
      }
    }

    for {
      field <- Try(unsafeField)
      direction <- toSortByDirection(sortBy.direction)
    } yield ResultOrdering(field, direction)
  }

  def toSortByDirection(proto: common_models.SortByDirection): Try[ResultOrdering.Direction] = {
    def unsafe = {
      proto match {
        case SortByDirection.SORT_BY_DIRECTION_UNKNOWN => ResultOrdering.Direction.Ascending
        case SortByDirection.SORT_BY_DIRECTION_ASCENDING => ResultOrdering.Direction.Ascending
        case SortByDirection.SORT_BY_DIRECTION_DESCENDING => ResultOrdering.Direction.Descending
        case SortByDirection.Unrecognized(x) => throw new RuntimeException(s"Unrecognized SortBy Direction: $x")
      }
    }

    Try(unsafe)
  }

  def toCredentialTypeStateProto(state: CredentialTypeState): console_models.CredentialTypeState = {
    state match {
      case CredentialTypeState.Archived => console_models.CredentialTypeState.CREDENTIAL_TYPE_ARCHIVED
      case CredentialTypeState.Draft => console_models.CredentialTypeState.CREDENTIAL_TYPE_DRAFT
      case CredentialTypeState.Ready => console_models.CredentialTypeState.CREDENTIAL_TYPE_READY
    }
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
      .withFieldConst(_.state, toCredentialTypeStateProto(credentialType.state))
      .withFieldConst(_.createdAt, credentialType.createdAt.toEpochMilli)
      .withFieldConst(_.id, credentialType.id.uuid.toString)
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

  def toUpdateContact(request: console_api.UpdateContactRequest): Try[UpdateContact] = {
    for {
      contactId <- Contact.Id.from(request.contactId)
      newExternalId <- Contact.ExternalId.validated(request.newExternalId)
      newName = request.newName.trim
      newJsonData <- JsonValidator.jsonData(request.newJsonData)
    } yield UpdateContact(contactId, newExternalId, newJsonData, newName)
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
      _ <-
        if (timeInterval.startTimestamp == 0)
          Failure(new IllegalArgumentException("Starting timestamp was not specified"))
        else
          Success(())
      _ <-
        if (timeInterval.endTimestamp == 0)
          Failure(new IllegalArgumentException("Ending timestamp was not specified"))
        else
          Success(())
      startTimestamp <- Try(Instant.ofEpochMilli(timeInterval.startTimestamp))
      endTimestamp <- Try(Instant.ofEpochMilli(timeInterval.endTimestamp))
      _ <-
        if (startTimestamp.isAfter(endTimestamp))
          Failure(
            new IllegalArgumentException("Starting timestamp cannot be after the ending timestamp")
          )
        else
          Success(())
    } yield TimeInterval(startTimestamp, endTimestamp)
  }
}
