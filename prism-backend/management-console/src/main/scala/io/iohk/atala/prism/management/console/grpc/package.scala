package io.iohk.atala.prism.management.console

import cats.syntax.traverse._
import com.google.protobuf.ByteString
import io.circe.Json
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs.{checkListUniqueness, toTimestamp}
import io.iohk.atala.prism.management.console.models.PaginatedQueryConstraints.ResultOrdering
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialBulk,
  CreateCredentialIssuance,
  GetCredentialIssuance
}
import io.iohk.atala.prism.management.console.validations.JsonValidator
import io.iohk.atala.prism.protos.{common_models, console_models}
import io.iohk.atala.prism.protos.common_models.SortByDirection
import io.iohk.atala.prism.protos.console_api._
import io.scalaland.chimney.dsl._
import io.scalaland.chimney.Transformer

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

package object grpc {

  private def maybeEmpty[T](value: String, f: String => Try[T]): Try[Option[T]] = {
    if (value.isEmpty)
      Success(None)
    else
      f(value).map(Option.apply)
  }

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val credentialTypeFieldTypeTransformer
      : Transformer[console_models.CredentialTypeFieldType, CredentialTypeFieldType] = {
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_STRING => CredentialTypeFieldType.String
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_INT => CredentialTypeFieldType.Int
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_BOOLEAN => CredentialTypeFieldType.Boolean
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_DATE => CredentialTypeFieldType.Date
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_UNKNOWN =>
      throw new IllegalArgumentException(
        s"Unknown credential type, allowed values: " +
          s"${console_models.CredentialTypeFieldType.values.map(_.name).mkString(", ")}"
      )
    case console_models.CredentialTypeFieldType.Unrecognized(unrecognizedValue) =>
      throw new IllegalArgumentException(
        s"Unrecognized credential type field type: $unrecognizedValue, allowed values: " +
          s"${console_models.CredentialTypeFieldType.values.map(_.name).mkString(", ")}"
      )
  }

  implicit val byteStringToOptionVectorByteTransformer: Transformer[ByteString, Option[Vector[Byte]]] =
    (byteString: ByteString) => {
      val iconByteArray = byteString.toByteArray
      if (iconByteArray.nonEmpty) Some(iconByteArray.toVector)
      else None
    }

  implicit val getStatisticsConverter: ProtoConverter[GetStatisticsRequest, GetStatistics] =
    (request: GetStatisticsRequest) => {
      request.interval match {
        case Some(protoInterval) =>
          toTimestamp(protoInterval).map(timeInterval => GetStatistics(Some(timeInterval)))
        case None =>
          Success(GetStatistics(None))
      }
    }

  implicit val createGroupConverter: ProtoConverter[CreateGroupRequest, CreateInstitutionGroup] =
    (request: CreateGroupRequest) => {
      for {
        contactIds <- request.contactIds.toList.map(Contact.Id.from).sequence
        contactIdsSet <- checkListUniqueness(contactIds)
        name = InstitutionGroup.Name(request.name)
      } yield CreateInstitutionGroup(name, contactIdsSet)
    }

  implicit val getGroupsConverter: ProtoConverter[GetGroupsRequest, GetInstitutionGroups] =
    (request: GetGroupsRequest) => {
      maybeEmpty(request.contactId, Contact.Id.from).map(GetInstitutionGroups)
    }

  implicit val updateGroupConverter: ProtoConverter[UpdateGroupRequest, UpdateInstitutionGroup] =
    (request: UpdateGroupRequest) => {
      for {
        groupId <- InstitutionGroup.Id.from(request.groupId)
        contactIdsToAdd <- request.contactIdsToAdd.toList.map(Contact.Id.from).sequence
        contactIdsToRemove <- request.contactIdsToRemove.toList.map(Contact.Id.from).sequence
        contactIdsToAddSet <- checkListUniqueness(contactIdsToAdd)
        contactIdsToRemoveSet <- checkListUniqueness(contactIdsToRemove)
        name <- maybeEmpty(request.name, s => Success(InstitutionGroup.Name(s)))
      } yield UpdateInstitutionGroup(groupId, contactIdsToAddSet, contactIdsToRemoveSet, name)
    }

  implicit val copyGroupConverter: ProtoConverter[CopyGroupRequest, CopyInstitutionGroup] =
    (request: CopyGroupRequest) =>
      for {
        groupToCopyId <- InstitutionGroup.Id.from(request.groupId)
        newName <-
          if (request.name.isEmpty)
            Failure(new IllegalArgumentException("New name is empty"))
          else
            Success(InstitutionGroup.Name(request.name))
      } yield CopyInstitutionGroup(groupToCopyId, newName)

  implicit val deleteGroupConverter: ProtoConverter[DeleteGroupRequest, DeleteInstitutionGroup] =
    (request: DeleteGroupRequest) => {
      InstitutionGroup.Id.from(request.groupId).map(DeleteInstitutionGroup)
    }

  implicit val createContactConverter: ProtoConverter[CreateContactRequest, CreateContact] =
    (request: CreateContactRequest) => {
      for {
        json <- JsonValidator.jsonData(request.jsonData)
        externalId <- Contact.ExternalId.validated(request.externalId)
        generateConnectionTokenRequestMetadata <-
          request.generateConnectionTokensRequestMetadata
            .map(_.transformInto[GenerateConnectionTokenRequestMetadata])
            .fold[Try[GenerateConnectionTokenRequestMetadata]](
              Failure(new IllegalArgumentException("generateConnectionTokenRequestMetadata is missing"))
            )(Success(_))
      } yield CreateContact(externalId, json, request.name, generateConnectionTokenRequestMetadata)
    }

  def toSortByDirection(proto: SortByDirection): Try[ResultOrdering.Direction] = {
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

  def toContactsResultOrdering(sortBy: GetContactsRequest.SortBy): Try[ResultOrdering[Contact.SortBy]] = {
    def unsafeField = {
      sortBy.field match {
        case GetContactsRequest.SortBy.Field.UNKNOWN => Contact.SortBy.createdAt
        case GetContactsRequest.SortBy.Field.CREATED_AT => Contact.SortBy.createdAt
        case GetContactsRequest.SortBy.Field.NAME => Contact.SortBy.name
        case GetContactsRequest.SortBy.Field.EXTERNAL_ID => Contact.SortBy.externalId
        case GetContactsRequest.SortBy.Field.Unrecognized(x) =>
          throw new RuntimeException(s"Unrecognized SortBy Field: $x")
      }
    }

    for {
      field <- Try(unsafeField)
      direction <- toSortByDirection(sortBy.direction)
    } yield ResultOrdering(field, direction)
  }

  implicit val getContactsConverter: ProtoConverter[GetContactsRequest, Contact.PaginatedQuery] =
    (request: GetContactsRequest) => {
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

  implicit val getContactConverter: ProtoConverter[GetContactRequest, GetContact] =
    (request: GetContactRequest) => {
      Contact.Id.from(request.contactId).map(GetContact)
    }

  implicit val updateContactConverter: ProtoConverter[UpdateContactRequest, UpdateContact] =
    (request: UpdateContactRequest) => {
      for {
        contactId <- Contact.Id.from(request.contactId)
        newExternalId <- Contact.ExternalId.validated(request.newExternalId)
        newName = request.newName.trim
        newJsonData <- JsonValidator.jsonData(request.newJsonData)
      } yield UpdateContact(contactId, newExternalId, newJsonData, newName)
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

  def toCreateContact(request: CreateContactsRequest.Contact): Try[CreateContact.NoOwner] = {
    for {
      json <- JsonValidator.jsonData(request.jsonData)
      externalId <- Contact.ExternalId.validated(request.externalId)
    } yield CreateContact.NoOwner(externalId, json, request.name)
  }

  def toCreateContacts(request: Seq[CreateContactsRequest.Contact]): Try[List[CreateContact.NoOwner]] = {
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

  implicit val createContactsConverter: ProtoConverter[CreateContactsRequest, CreateContact.Batch] =
    (request: CreateContactsRequest) => {
      for {
        validatedGroups <- toGroupIdSet(request.groups)
        validatedContacts <- toCreateContacts(request.contacts)
        _ = if (validatedContacts.isEmpty) throw new RuntimeException("There are no contacts to create")
        generateConnectionTokenRequestMetadata <-
          request.generateConnectionTokensRequestMetadata
            .map(_.transformInto[GenerateConnectionTokenRequestMetadata])
            .fold[Try[GenerateConnectionTokenRequestMetadata]](
              Failure(new IllegalArgumentException("generateConnectionTokenRequestMetadata is missing"))
            )(Success(_))
      } yield CreateContact.Batch(validatedGroups, validatedContacts, generateConnectionTokenRequestMetadata)
    }

  implicit val deleteContactConverter: ProtoConverter[DeleteContactRequest, DeleteContact] =
    (request: DeleteContactRequest) => {
      Contact.Id.from(request.contactId).map(DeleteContact)
    }

  implicit val createCredentialIssuanceConverter
      : ProtoConverter[CreateCredentialIssuanceRequest, CreateCredentialIssuance] =
    (request: CreateCredentialIssuanceRequest) => {
      for {
        credentialTypeId <- CredentialTypeId.from(request.credentialTypeId)
        contacts <-
          request.credentialIssuanceContacts
            .map { contact =>
              for {
                contactId <- Contact.Id.from(contact.contactId)
                credentialData <-
                  io.circe.parser
                    .parse(contact.credentialData)
                    .toTry
                groupIds <-
                  contact.groupIds
                    .map { groupId =>
                      InstitutionGroup.Id.from(groupId)
                    }
                    .to(List)
                    .sequence
              } yield CredentialIssuancesRepository.CreateCredentialIssuanceContact(
                contactId,
                credentialData,
                groupIds
              )
            }
            .to(List)
            .sequence
      } yield CredentialIssuancesRepository.CreateCredentialIssuance(
        request.name,
        credentialTypeId,
        contacts
      )
    }

  implicit val getCredentialIssuanceConverter: ProtoConverter[GetCredentialIssuanceRequest, GetCredentialIssuance] =
    (request: GetCredentialIssuanceRequest) => {
      CredentialIssuance.Id.from(request.credentialIssuanceId).map(GetCredentialIssuance)
    }

  implicit val createGenericCredentialConverter
      : ProtoConverter[CreateGenericCredentialRequest, CreateGenericCredential] =
    (request: CreateGenericCredentialRequest) => {
      for {
        contactId <- maybeEmpty(request.contactId, Contact.Id.from)
        credentialData <- io.circe.parser.parse(request.credentialData).toTry
        externalId <- maybeEmpty(request.externalId, Contact.ExternalId.validated)
        credentialTypeId <-
          if (request.credentialTypeId.nonEmpty) CredentialTypeId.from(request.credentialTypeId)
          else Failure(new IllegalArgumentException("Empty credential type id"))
      } yield CreateGenericCredential(
        contactId,
        credentialData,
        externalId,
        None,
        credentialTypeId
      )
    }

  implicit val getGenericCredentialConverter: ProtoConverter[GetGenericCredentialsRequest, GetGenericCredential] =
    (request: GetGenericCredentialsRequest) => {
      for {
        lastSeenCredentialId <- maybeEmpty(request.lastSeenCredentialId, GenericCredential.Id.from)
      } yield GetGenericCredential(
        request.limit,
        lastSeenCredentialId
      )
    }

  implicit val getContactCredentialConverter: ProtoConverter[GetContactCredentialsRequest, GetContactCredentials] =
    (request: GetContactCredentialsRequest) => {
      Contact.Id.from(request.contactId).map(GetContactCredentials)
    }

  implicit val shareCredentialConverter: ProtoConverter[ShareCredentialRequest, ShareCredential] =
    (request: ShareCredentialRequest) => {
      GenericCredential.Id.from(request.cmanagerCredentialId).map(ShareCredential)
    }

  implicit val createCredentialBulkConverter: ProtoConverter[CreateGenericCredentialBulkRequest, CreateCredentialBulk] =
    (request: CreateGenericCredentialBulkRequest) => {
      for {
        json <- io.circe.parser.parse(request.credentialsJSON).toTry
        draftsJson <- JsonValidator.extractField[List[Json]](json)("drafts")
        drafts <- draftsJson.map { draftJson =>
          for {
            externalId <- JsonValidator.extractFieldWithTry[String, Contact.ExternalId](draftJson)("external_id")(
              Contact.ExternalId.validated
            )
            credentialJson <- JsonValidator.extractField[Json](draftJson)("credential_data")
            groupIds <-
              JsonValidator.extractFieldWithTry[List[String], Set[InstitutionGroup.Id]](draftJson)("group_ids")(
                _.map(InstitutionGroup.Id.from).sequence.map(_.toSet)
              )
          } yield CreateCredentialBulk.Draft(externalId, credentialJson, groupIds)
        }.sequence
        credentialsType <-
          JsonValidator
            .extractFieldWith[String, CredentialTypeId](json)("credential_type_id")(
              CredentialTypeId.unsafeFrom
            )
        issuanceName <- JsonValidator.extractField[String](json)("issuance_name")
        _ <-
          if (issuanceName.isEmpty)
            Failure(new IllegalArgumentException("Empty issuance name"))
          else
            Success(())
      } yield CreateCredentialBulk(json, drafts, credentialsType, issuanceName)
    }

  implicit val storeCredentialConverter: ProtoConverter[StoreCredentialRequest, StoreCredential] =
    (request: StoreCredentialRequest) => {
      for {
        contactId <- Contact.Id.from(request.connectionId)
        credentialExternalId <- CredentialExternalId.from(request.credentialExternalId)
      } yield StoreCredential(
        contactId,
        request.encodedSignedCredential,
        credentialExternalId
      )
    }

  implicit val getLatestCredentialsConverter
      : ProtoConverter[GetLatestCredentialExternalIdRequest, GetLatestCredential] =
    (_: GetLatestCredentialExternalIdRequest) => {
      Success(GetLatestCredential())
    }

  implicit val getStoredCredentialsConverter: ProtoConverter[GetStoredCredentialsForRequest, GetStoredCredentials] =
    (request: GetStoredCredentialsForRequest) => {
      Contact.Id.from(request.individualId).map(GetStoredCredentials)
    }

  implicit val getCredentialTypesConverter: ProtoConverter[GetCredentialTypesRequest, GetCredentialTypes] =
    (_: GetCredentialTypesRequest) => Success(GetCredentialTypes())

  implicit val getCredentialTypeConverter: ProtoConverter[GetCredentialTypeRequest, GetCredentialType] =
    (request: GetCredentialTypeRequest) => {
      CredentialTypeId.from(request.credentialTypeId).map(GetCredentialType)
    }

  implicit val createCredentialTypeConverter: ProtoConverter[CreateCredentialTypeRequest, CreateCredentialType] =
    (request: CreateCredentialTypeRequest) => {
      request.credentialType.toRight(new IllegalArgumentException("Empty credentialType field")).toTry.flatMap {
        credentialType =>
          Try(credentialType.into[CreateCredentialType].transform)
      }
    }

  implicit val updateCredentialTypeConverter: ProtoConverter[UpdateCredentialTypeRequest, UpdateCredentialType] =
    (request: UpdateCredentialTypeRequest) => {
      for {
        updateCredentialType <-
          request.credentialType.toRight(new IllegalArgumentException("Empty credentialType field")).toTry
        credentialTypeId <- CredentialTypeId.from(updateCredentialType.id)
        updateCredentialTypeTransformed <- Try {
          updateCredentialType
            .into[UpdateCredentialType]
            .withFieldConst(_.id, credentialTypeId)
            .transform
        }
      } yield updateCredentialTypeTransformed
    }

  implicit val markAsReadyConverter: ProtoConverter[MarkAsReadyCredentialTypeRequest, MarkAsReadyCredentialType] =
    (request: MarkAsReadyCredentialTypeRequest) => {
      CredentialTypeId.from(request.credentialTypeId).map(MarkAsReadyCredentialType)
    }

  implicit val markAsArchivedConverter
      : ProtoConverter[MarkAsArchivedCredentialTypeRequest, MarkAsArchivedCredentialType] =
    (request: MarkAsArchivedCredentialTypeRequest) => {
      CredentialTypeId.from(request.credentialTypeId).map(MarkAsArchivedCredentialType)
    }
}
