package io.iohk.atala.prism.management.console

import cats.data.NonEmptyList
import cats.implicits.catsSyntaxTuple4Semigroupal
import cats.syntax.traverse._
import com.google.protobuf.ByteString
import io.circe.Json
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeader
import io.iohk.atala.prism.auth.model.RequestNonce
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.grpc.ProtoConverter
import io.iohk.atala.prism.identity.{CanonicalPrismDid, LongFormPrismDid, PrismDid}
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
import io.iohk.atala.prism.credentials.CredentialBatchId
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.prism.crypto.Sha256Digest
import io.iohk.atala.prism.crypto.signature.ECSignature
import io.iohk.atala.prism.models.ConnectionToken
import io.iohk.atala.prism.protos.connector_api.SendMessagesRequest
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.node_api.ScheduleOperationsResponse
import io.iohk.atala.prism.utils.Base64Utils

import scala.util.{Failure, Success, Try}

package object grpc {

  private def maybeEmpty[T](
      value: String,
      f: String => Try[T]
  ): Try[Option[T]] = {
    if (value.isEmpty)
      Success(None)
    else
      f(value).map(Option.apply)
  }

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val credentialTypeFieldTypeTransformer: Transformer[
    console_models.CredentialTypeFieldType,
    CredentialTypeFieldType
  ] = {
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_STRING =>
      CredentialTypeFieldType.String
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_INT =>
      CredentialTypeFieldType.Int
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_BOOLEAN =>
      CredentialTypeFieldType.Boolean
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_DATE =>
      CredentialTypeFieldType.Date
    case console_models.CredentialTypeFieldType.CREDENTIAL_TYPE_FIELD_UNKNOWN =>
      throw new IllegalArgumentException(
        s"Unknown credential type, allowed values: " +
          s"${console_models.CredentialTypeFieldType.values.map(_.name).mkString(", ")}"
      )
    case console_models.CredentialTypeFieldType.Unrecognized(
          unrecognizedValue
        ) =>
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
    (request: GetStatisticsRequest, _) => {
      request.interval match {
        case Some(protoInterval) =>
          toTimestamp(protoInterval).map(timeInterval => GetStatistics(Some(timeInterval)))
        case None =>
          Success(GetStatistics(None))
      }
    }

  implicit val registerDIDConverted: ProtoConverter[RegisterConsoleDIDRequest, RegisterDID] = { (request, _) =>
    {
      for {
        did <- Try {
          Try(
            PrismDid
              .fromString(request.did)
          ).getOrElse(throw new RuntimeException("Missing or invalid DID"))
        }
        name <- Try {
          if (request.name.trim.isEmpty)
            throw new RuntimeException("The name is required")
          else request.name.trim
        }

        logo <- Try {
          val bytes = request.logo.toByteArray
          ParticipantLogo(bytes.toVector)
        }
      } yield RegisterDID(did = did, name = name, logo = logo)
    }
  }

  implicit val createGroupConverter: ProtoConverter[CreateGroupRequest, CreateInstitutionGroup] =
    (request: CreateGroupRequest, _) => {
      for {
        contactIds <- request.contactIds.toList.map(Contact.Id.from).sequence
        contactIdsSet <- checkListUniqueness(contactIds)
        name = InstitutionGroup.Name(request.name)
      } yield CreateInstitutionGroup(name, contactIdsSet)
    }

  def toInstitutionGroupsResultOrdering(
      sortBy: GetGroupsRequest.SortBy
  ): Try[ResultOrdering[InstitutionGroup.SortBy]] = {
    def unsafeField = {
      sortBy.field match {
        case GetGroupsRequest.SortBy.Field.UNKNOWN =>
          InstitutionGroup.SortBy.Name
        case GetGroupsRequest.SortBy.Field.NAME => InstitutionGroup.SortBy.Name
        case GetGroupsRequest.SortBy.Field.CREATED_AT =>
          InstitutionGroup.SortBy.CreatedAt
        case GetGroupsRequest.SortBy.Field.NUMBER_OF_CONTACTS =>
          InstitutionGroup.SortBy.NumberOfContacts
        case GetGroupsRequest.SortBy.Field.Unrecognized(x) =>
          throw new RuntimeException(s"Unrecognized SortBy Field: $x")
      }
    }

    for {
      field <- Try(unsafeField)
      direction <- toSortByDirection(sortBy.direction)
    } yield ResultOrdering(field, direction)
  }

  implicit val getGroupsConverter: ProtoConverter[GetGroupsRequest, InstitutionGroup.PaginatedQuery] =
    (request: GetGroupsRequest, _) => {
      val createdAfterTry = Try {
        request.filterBy
          .flatMap(_.createdAfter)
          .map(proto2DateTransformer.transform)
      }

      val createdBeforeTry = Try {
        request.filterBy
          .flatMap(_.createdBefore)
          .map(proto2DateTransformer.transform)
      }

      val contactIdTry = request.filterBy
        .map(_.contactId)
        .map(Contact.Id.optional)
        .getOrElse(Try(None))
      val name =
        request.filterBy.map(_.name).flatMap(InstitutionGroup.Name.optional)

      val defaultSortBy =
        ResultOrdering[InstitutionGroup.SortBy](InstitutionGroup.SortBy.Name)
      val sortByT: Try[ResultOrdering[InstitutionGroup.SortBy]] =
        request.sortBy
          .map(toInstitutionGroupsResultOrdering)
          .getOrElse(Try(defaultSortBy))
      val allowedLimit = 0 to 100
      val defaultLimit = 10
      val limitT = Try {
        if (allowedLimit contains request.limit) request.limit
        else
          throw new RuntimeException(
            s"Invalid limit, allowed values are $allowedLimit"
          )
      }.map {
        case 0 => defaultLimit
        case x => x
      }

      val offsetT =
        if (request.offset >= 0) Success(request.offset)
        else
          Failure(
            new IllegalArgumentException("offset cannot be negative number")
          )

      for {
        createdAfter <- createdAfterTry
        createdBefore <- createdBeforeTry
        sortBy <- sortByT
        limit <- limitT
        contactId <- contactIdTry
        offset <- offsetT
      } yield PaginatedQueryConstraints(
        limit = limit,
        offset = offset,
        ordering = sortBy,
        scrollId = None,
        filters = Some(
          InstitutionGroup.FilterBy(
            name = name,
            createdAfter = createdAfter,
            createdBefore = createdBefore,
            contactId = contactId
          )
        )
      )
    }

  implicit val updateGroupConverter: ProtoConverter[UpdateGroupRequest, UpdateInstitutionGroup] =
    (request: UpdateGroupRequest, _) => {
      for {
        groupId <- InstitutionGroup.Id.from(request.groupId)
        contactIdsToAdd <- request.contactIdsToAdd.toList
          .map(Contact.Id.from)
          .sequence
        contactIdsToRemove <- request.contactIdsToRemove.toList
          .map(Contact.Id.from)
          .sequence
        contactIdsToAddSet <- checkListUniqueness(contactIdsToAdd)
        contactIdsToRemoveSet <- checkListUniqueness(contactIdsToRemove)
        name <- maybeEmpty(request.name, s => Success(InstitutionGroup.Name(s)))
      } yield UpdateInstitutionGroup(
        groupId,
        contactIdsToAddSet,
        contactIdsToRemoveSet,
        name
      )
    }

  implicit val copyGroupConverter: ProtoConverter[CopyGroupRequest, CopyInstitutionGroup] =
    (request: CopyGroupRequest, _) =>
      for {
        groupToCopyId <- InstitutionGroup.Id.from(request.groupId)
        newName <-
          if (request.name.isEmpty)
            Failure(new IllegalArgumentException("New name is empty"))
          else
            Success(InstitutionGroup.Name(request.name))
      } yield CopyInstitutionGroup(groupToCopyId, newName)

  implicit val deleteGroupConverter: ProtoConverter[DeleteGroupRequest, DeleteInstitutionGroup] =
    (request: DeleteGroupRequest, _) => {
      InstitutionGroup.Id.from(request.groupId).map(DeleteInstitutionGroup)
    }

  implicit val createContactConverter: ProtoConverter[CreateContactRequest, CreateContact] =
    (request: CreateContactRequest, _) => {
      for {
        json <- JsonValidator.jsonData(request.jsonData)
        externalId <- Contact.ExternalId.validated(request.externalId)
        generateConnectionTokenRequestMetadata <- toConnectorRequestMetadata(
          request.generateConnectionTokensRequestMetadata
        )
      } yield CreateContact(
        externalId,
        json,
        request.name,
        generateConnectionTokenRequestMetadata
      )
    }

  def toSortByDirection(
      proto: SortByDirection
  ): Try[ResultOrdering.Direction] = {
    def unsafe = {
      proto match {
        case SortByDirection.SORT_BY_DIRECTION_UNKNOWN =>
          ResultOrdering.Direction.Ascending
        case SortByDirection.SORT_BY_DIRECTION_ASCENDING =>
          ResultOrdering.Direction.Ascending
        case SortByDirection.SORT_BY_DIRECTION_DESCENDING =>
          ResultOrdering.Direction.Descending
        case SortByDirection.Unrecognized(x) =>
          throw new RuntimeException(s"Unrecognized SortBy Direction: $x")
      }
    }

    Try(unsafe)
  }

  def toContactsResultOrdering(
      sortBy: GetContactsRequest.SortBy
  ): Try[ResultOrdering[Contact.SortBy]] = {
    def unsafeField = {
      sortBy.field match {
        case GetContactsRequest.SortBy.Field.UNKNOWN => Contact.SortBy.createdAt
        case GetContactsRequest.SortBy.Field.CREATED_AT =>
          Contact.SortBy.createdAt
        case GetContactsRequest.SortBy.Field.NAME => Contact.SortBy.name
        case GetContactsRequest.SortBy.Field.EXTERNAL_ID =>
          Contact.SortBy.externalId
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
    (request: GetContactsRequest, _) => {
      val scrollIdT = Contact.Id.optional(request.scrollId)
      val createdAtT = Try {
        request.filterBy
          .flatMap(_.createdAt)
          .map(proto2DateTransformer.transform)
      }

      val nameOrExternalId = request.filterBy
        .map(_.nameOrExternalId)
        .map(_.trim)
        .filter(_.nonEmpty)
      val groupName = request.filterBy
        .map(_.groupName)
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(InstitutionGroup.Name.apply)

      val defaultSortBy = ResultOrdering(Contact.SortBy.createdAt)
      val sortByT = request.sortBy
        .map(toContactsResultOrdering)
        .getOrElse(Try(defaultSortBy))
      val allowedLimit = 0 to 100
      val defaultLimit = 10
      val limitT = Try {
        if (allowedLimit contains request.limit) request.limit
        else
          throw new RuntimeException(
            s"Invalid limit, allowed values are $allowedLimit"
          )
      }.map {
        case 0 => defaultLimit
        case x => x
      }

      // filterNot(_.isStatusMissing) fixes a weird bug, given that the proto status gets propagated
      // to the database layer, the missing status should be translated to None, otherwise, the db will
      // look for specific rows with such status (there are none).
      val connectionStatus: Option[ContactConnectionStatus] = request.filterBy
        .map(_.connectionStatus)
        .filterNot(_.isStatusMissing)

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
            nameOrExternalId = nameOrExternalId,
            connectionStatus = connectionStatus
          )
        )
      )
    }

  implicit val getContactConverter: ProtoConverter[GetContactRequest, GetContact] =
    (request: GetContactRequest, _) => {
      Contact.Id.from(request.contactId).map(GetContact)
    }

  implicit val updateContactConverter: ProtoConverter[UpdateContactRequest, UpdateContact] =
    (request: UpdateContactRequest, _) => {
      for {
        contactId <- Contact.Id.from(request.contactId)
        newExternalId <- Contact.ExternalId.validated(request.newExternalId)
        newName = request.newName.trim
        newJsonData <- JsonValidator.jsonData(request.newJsonData)
      } yield UpdateContact(contactId, newExternalId, newJsonData, newName)
    }

  def toGroupIdSet(request: Seq[String]): Try[Set[InstitutionGroup.Id]] = {
    val validatedGroups =
      request.map(InstitutionGroup.Id.from).flatMap(_.toOption).toSet
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

  def toCreateContact(
      request: CreateContactsRequest.Contact
  ): Try[CreateContact.NoOwner] = {
    for {
      json <- JsonValidator.jsonData(request.jsonData)
      externalId <- Contact.ExternalId.validated(request.externalId)
    } yield CreateContact.NoOwner(externalId, json, request.name)
  }

  def toCreateContacts(
      request: Seq[CreateContactsRequest.Contact]
  ): Try[List[CreateContact.NoOwner]] = {
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
    (request: CreateContactsRequest, _) => {
      for {
        validatedGroups <- toGroupIdSet(request.groups)
        validatedContacts <- toCreateContacts(request.contacts)
        _ = if (validatedContacts.isEmpty)
          throw new RuntimeException("There are no contacts to create")
        generateConnectionTokenRequestMetadata <- toConnectorRequestMetadata(
          request.generateConnectionTokensRequestMetadata
        )
      } yield CreateContact.Batch(
        validatedGroups,
        validatedContacts,
        generateConnectionTokenRequestMetadata
      )
    }

  implicit val deleteContactConverter: ProtoConverter[DeleteContactRequest, DeleteContact] =
    (request: DeleteContactRequest, _) => {
      Contact.Id.from(request.contactId).map(DeleteContact)
    }

  implicit val createCredentialIssuanceConverter: ProtoConverter[
    CreateCredentialIssuanceRequest,
    CreateCredentialIssuance
  ] =
    (request: CreateCredentialIssuanceRequest, _) => {
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
              } yield CredentialIssuancesRepository
                .CreateCredentialIssuanceContact(
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
    (request: GetCredentialIssuanceRequest, _) => {
      CredentialIssuance.Id
        .from(request.credentialIssuanceId)
        .map(GetCredentialIssuance)
    }

  implicit val createGenericCredentialConverter: ProtoConverter[
    CreateGenericCredentialRequest,
    CreateGenericCredential
  ] =
    (request: CreateGenericCredentialRequest, _) => {
      for {
        contactId <- maybeEmpty(request.contactId, Contact.Id.from)
        credentialData <- io.circe.parser.parse(request.credentialData).toTry
        externalId <- maybeEmpty(
          request.externalId,
          Contact.ExternalId.validated
        )
        credentialTypeId <-
          if (request.credentialTypeId.nonEmpty)
            CredentialTypeId.from(request.credentialTypeId)
          else Failure(new IllegalArgumentException("Empty credential type id"))
      } yield CreateGenericCredential(
        contactId,
        credentialData,
        externalId,
        None,
        credentialTypeId
      )
    }

  def toGenericCredentialResultOrdering(
      sortBy: GetGenericCredentialsRequest.SortBy
  ): Try[ResultOrdering[GenericCredential.SortBy]] = {
    def unsafeField = {
      sortBy.field match {
        case GetGenericCredentialsRequest.SortBy.Field.UNKNOWN =>
          GenericCredential.SortBy.CreatedOn
        case GetGenericCredentialsRequest.SortBy.Field.CREDENTIAL_TYPE =>
          GenericCredential.SortBy.CredentialType
        case GetGenericCredentialsRequest.SortBy.Field.CREATED_ON =>
          GenericCredential.SortBy.CreatedOn
        case GetGenericCredentialsRequest.SortBy.Field.Unrecognized(x) =>
          throw new RuntimeException(s"Unrecognized SortBy Field: $x")
      }
    }

    for {
      field <- Try(unsafeField)
      direction <- toSortByDirection(sortBy.direction)
    } yield ResultOrdering(field, direction)
  }

  implicit val getGenericCredentialConverter: ProtoConverter[
    GetGenericCredentialsRequest,
    GenericCredential.PaginatedQuery
  ] =
    (request: GetGenericCredentialsRequest, _) => {
      val createdAfterT = Try {
        request.filterBy
          .flatMap(_.createdAfter)
          .map(proto2DateTransformer.transform)
      }

      val createdBeforeT = Try {
        request.filterBy
          .flatMap(_.createdBefore)
          .map(proto2DateTransformer.transform)
      }

      val credentialTypeT =
        request.filterBy
          .map(_.credentialType)
          .map(CredentialTypeId.optional)
          .getOrElse(Success(None))

      val defaultSortBy = ResultOrdering[GenericCredential.SortBy](
        GenericCredential.SortBy.CreatedOn
      )
      val sortByT: Try[ResultOrdering[GenericCredential.SortBy]] =
        request.sortBy
          .map(toGenericCredentialResultOrdering)
          .getOrElse(Success(defaultSortBy))
      val allowedLimit = 0 to 100
      val defaultLimit = 10
      val limitT = Try {
        if (allowedLimit contains request.limit) request.limit
        else
          throw new RuntimeException(
            s"Invalid limit, allowed values are $allowedLimit"
          )
      }.map {
        case 0 => defaultLimit
        case x => x
      }

      val offsetT =
        if (request.offset >= 0) Success(request.offset)
        else
          Failure(
            new IllegalArgumentException("offset cannot be negative number")
          )

      for {
        createdAfter <- createdAfterT
        createdBefore <- createdBeforeT
        sortBy <- sortByT
        limit <- limitT
        credentialType <- credentialTypeT
        offset <- offsetT
      } yield PaginatedQueryConstraints(
        limit = limit,
        offset = offset,
        ordering = sortBy,
        scrollId = None,
        filters = Some(
          GenericCredential.FilterBy(
            credentialType = credentialType,
            createdAfter = createdAfter,
            createdBefore = createdBefore
          )
        )
      )
    }

  implicit val getContactCredentialConverter: ProtoConverter[GetContactCredentialsRequest, GetContactCredentials] =
    (request: GetContactCredentialsRequest, _) => {
      Contact.Id.from(request.contactId).map(GetContactCredentials)
    }

  implicit val shareCredentialConverter: ProtoConverter[ShareCredentialRequest, ShareCredential] =
    (request: ShareCredentialRequest, _) => {
      GenericCredential.Id
        .from(request.cmanagerCredentialId)
        .map(ShareCredential)
    }

  implicit val shareCredentialsConverter: ProtoConverter[ShareCredentialsRequest, ShareCredentials] =
    (request: ShareCredentialsRequest, _) => {
      for {
        idsNonEmptyList <- toCredentialsIds(request.credentialsIds)
        connectorRequestMetadata <- toConnectorRequestMetadata(
          request.sendMessagesRequestMetadata
        )
        sendMessageRequest <- request.sendMessagesRequest
          .fold[Try[SendMessagesRequest]](
            Failure(
              new IllegalArgumentException(
                "sendMessagesRequest cannot be empty"
              )
            )
          )(Success(_))
        _ <-
          if (idsNonEmptyList.size == sendMessageRequest.messagesByConnectionToken.size) Success(())
          else
            Failure(
              new IllegalArgumentException(
                s"Number of credentialIds: ${idsNonEmptyList.size} doesn't equal number of" +
                  s" messages to send: ${sendMessageRequest.messagesByConnectionToken.size}"
              )
            )
      } yield ShareCredentials(
        idsNonEmptyList,
        sendMessageRequest,
        connectorRequestMetadata
      )
    }

  implicit val deleteCredentialsConverter: ProtoConverter[DeleteCredentialsRequest, DeleteCredentials] =
    (request: DeleteCredentialsRequest, _) => {
      for {
        idsNonEmptyList <- toCredentialsIds(request.credentialsIds)
      } yield DeleteCredentials(
        idsNonEmptyList
      )
    }

  private def toConnectorRequestMetadata(
      connectorRequestMetadata: Option[console_models.ConnectorRequestMetadata]
  ): Try[GrpcAuthenticationHeader.DIDBased] = {

    (
      connectorRequestMetadata.map(_.requestNonce),
      connectorRequestMetadata.map(_.did),
      connectorRequestMetadata.map(_.didKeyId),
      connectorRequestMetadata.map(_.didSignature)
    ).mapN { case (nonce, didStr, keyId, signature) =>
      Try(
        PrismDid
          .fromString(didStr)
      ).toOption
        .map { did =>
          val didBased = did match {
            case _: CanonicalPrismDid =>
              GrpcAuthenticationHeader.PublishedDIDBased
            case _: LongFormPrismDid =>
              GrpcAuthenticationHeader.UnpublishedDIDBased
            case _ => throw new RuntimeException("Unknown Did")
          }

          didBased(
            RequestNonce(Base64Utils.decodeURL(nonce).toVector),
            did,
            keyId,
            new ECSignature(Base64Utils.decodeURL(signature))
          )
        }
    }.flatten
      .fold[Try[GrpcAuthenticationHeader.DIDBased]](
        Failure(
          new IllegalArgumentException("connector request metadata is missing")
        )
      )(Success.apply)
  }

  private def toCredentialsIds(
      credentialsIds: Seq[String]
  ): Try[NonEmptyList[GenericCredential.Id]] = {
    for {
      idsList <- credentialsIds.map(GenericCredential.Id.from).toList.sequence
      idsNonEmptyList <-
        NonEmptyList
          .fromList(idsList)
          .map(Success(_))
          .getOrElse(
            Failure(new IllegalArgumentException("Empty credential ids list"))
          )
    } yield idsNonEmptyList
  }

  implicit val getLedgerDataConverter: ProtoConverter[GetLedgerDataRequest, GetLedgerData] =
    (request: GetLedgerDataRequest, _) => {
      for {
        batchId <- Try(CredentialBatchId.fromString(request.batchId))
        credentialHash = Sha256Digest.fromBytes(
          request.credentialHash.toByteArray
        )
      } yield GetLedgerData(batchId, credentialHash)
    }

  implicit val publishBatchConverter: ProtoConverter[PublishBatchRequest, PublishBatch] =
    (request: PublishBatchRequest, _) => {
      for {
        signedOperation <- Try(
          request.issueCredentialBatchOperation.getOrElse(
            throw new RuntimeException("Missing signed operation")
          )
        )
        isIssueCredentialBatch =
          signedOperation.operation
            .getOrElse(throw new RuntimeException("Missing operation"))
            .operation
            .isIssueCredentialBatch
        _ =
          if (isIssueCredentialBatch) ()
          else
            throw new RuntimeException(
              "IssueCredentialBatch operation expected but not found"
            )
      } yield PublishBatch(signedOperation)
    }

  implicit val storePublishedCredentialConverter: ProtoConverter[
    StorePublishedCredentialRequest,
    StorePublishedCredential
  ] =
    (request: StorePublishedCredentialRequest, _) => {
      for {
        encodedSignedCredential <- Try {
          require(
            request.encodedSignedCredential.nonEmpty,
            "Empty encoded credential"
          )
          request.encodedSignedCredential
        }
        consoleCredentialId = GenericCredential.Id.unsafeFrom(
          request.consoleCredentialId
        )
        batchId = CredentialBatchId.fromString(request.batchId)
        proof = Try(
          MerkleInclusionProof
            .decode(request.encodedInclusionProof)
        ).getOrElse(
          throw new RuntimeException(
            s"Invalid inclusion proof: ${request.encodedInclusionProof}"
          )
        )
      } yield StorePublishedCredential(
        encodedSignedCredential = encodedSignedCredential,
        consoleCredentialId = consoleCredentialId,
        batchId = batchId,
        inclusionProof = proof
      )
    }

  implicit val revokePublishedCredentialConverter: ProtoConverter[
    RevokePublishedCredentialRequest,
    RevokePublishedCredential
  ] =
    (request: RevokePublishedCredentialRequest, _) => {
      for {
        credentialId <- GenericCredential.Id.from(request.credentialId)
        operation =
          request.revokeCredentialsOperation
            .getOrElse(
              throw new RuntimeException("Missing revokeCredentialsOperation")
            )
        _ = if (!operation.operation.exists(_.operation.isRevokeCredentials))
          throw new RuntimeException(
            "Invalid revokeCredentialsOperation, it is a different operation"
          )

        credentialHashes =
          operation.operation
            .flatMap(_.operation.revokeCredentials)
            .map(_.credentialsToRevoke)
            .getOrElse(Seq.empty)
        _ = if (credentialHashes.isEmpty)
          throw new RuntimeException(
            "Invalid revokeCredentialsOperation, a single credential is expected but the whole batch was found"
          )
        _ = if (credentialHashes.size > 1)
          throw new RuntimeException(
            s"Invalid revokeCredentialsOperation, a single credential is expected but ${credentialHashes.size} credentials found"
          )
      } yield RevokePublishedCredential(credentialId, operation)
    }

  implicit val nodeRevocationResponse: ProtoConverter[
    ScheduleOperationsResponse,
    NodeRevocationResponse
  ] =
    (response: ScheduleOperationsResponse, _) =>
      Try {
        NodeRevocationResponse(
          AtalaOperationId.fromVectorUnsafe(
            response.outputs.head.getOperationId.toByteArray.toVector
          )
        )
      }

  implicit val issueCredentialBatchResponseConverter: ProtoConverter[
    ScheduleOperationsResponse,
    IssueCredentialBatchNodeResponse
  ] =
    (response: ScheduleOperationsResponse, _) => {
      for {
        batchId <- Try(
          Option(
            CredentialBatchId
              .fromString(response.outputs.head.getBatchOutput.batchId)
          ).getOrElse(
            throw new RuntimeException("Node returned an invalid batch id")
          )
        )
      } yield IssueCredentialBatchNodeResponse(
        batchId,
        AtalaOperationId.fromVectorUnsafe(
          response.outputs.head.getOperationId.toByteArray.toVector
        )
      )
    }

  implicit val createCredentialBulkConverter: ProtoConverter[
    CreateGenericCredentialBulkRequest,
    CreateCredentialBulk
  ] =
    (request: CreateGenericCredentialBulkRequest, _) => {
      for {
        json <- io.circe.parser.parse(request.credentialsJson).toTry
        draftsJson <- JsonValidator.extractField[List[Json]](json)("drafts")
        drafts <- draftsJson.map { draftJson =>
          for {
            externalId <- JsonValidator
              .extractFieldWithTry[String, Contact.ExternalId](draftJson)(
                "external_id"
              )(
                Contact.ExternalId.validated
              )
            credentialJson <- JsonValidator.extractField[Json](draftJson)(
              "credential_data"
            )
            groupIds <-
              JsonValidator
                .extractFieldWithTry[List[String], Set[InstitutionGroup.Id]](
                  draftJson
                )("group_ids")(
                  _.map(InstitutionGroup.Id.from).sequence.map(_.toSet)
                )
          } yield CreateCredentialBulk.Draft(
            externalId,
            credentialJson,
            groupIds
          )
        }.sequence
        credentialsType <-
          JsonValidator
            .extractFieldWith[String, CredentialTypeId](json)(
              "credential_type_id"
            )(
              CredentialTypeId.unsafeFrom
            )
        issuanceName <- JsonValidator.extractField[String](json)(
          "issuance_name"
        )
        _ <-
          if (issuanceName.isEmpty)
            Failure(new IllegalArgumentException("Empty issuance name"))
          else
            Success(())
      } yield CreateCredentialBulk(json, drafts, credentialsType, issuanceName)
    }

  implicit val storeCredentialConverter: ProtoConverter[StoreCredentialRequest, StoreCredential] =
    (request: StoreCredentialRequest, _) => {
      for {
        credentialExternalId <- CredentialExternalId.from(
          request.credentialExternalId
        )
        connectionToken = ConnectionToken(request.connectionToken)
      } yield StoreCredential(
        connectionToken,
        request.encodedSignedCredential,
        credentialExternalId
      )
    }

  implicit val getLatestCredentialsConverter: ProtoConverter[
    GetLatestCredentialExternalIdRequest,
    GetLatestCredential
  ] =
    (_: GetLatestCredentialExternalIdRequest, _) => {
      Success(GetLatestCredential())
    }

  implicit val getStoredCredentialsConverter: ProtoConverter[GetStoredCredentialsForRequest, GetStoredCredentials] =
    (request: GetStoredCredentialsForRequest, _) => {
      Contact.Id
        .optional(request.individualId)
        .map(GetStoredCredentials.FilterBy.apply)
        .map(GetStoredCredentials.apply)
    }

  implicit val getCredentialTypesConverter: ProtoConverter[GetCredentialTypesRequest, GetCredentialTypes] =
    (_: GetCredentialTypesRequest, _) => Success(GetCredentialTypes())

  implicit val getCredentialTypeConverter: ProtoConverter[GetCredentialTypeRequest, GetCredentialType] =
    (request: GetCredentialTypeRequest, _) => {
      CredentialTypeId.from(request.credentialTypeId).map(GetCredentialType)
    }

  implicit val createCredentialTypeConverter: ProtoConverter[CreateCredentialTypeRequest, CreateCredentialType] =
    (request: CreateCredentialTypeRequest, _) => {
      request.credentialType
        .toRight(new IllegalArgumentException("Empty credentialType field"))
        .toTry
        .flatMap { credentialType =>
          Try(credentialType.into[CreateCredentialType].transform)
        }
    }

  implicit val updateCredentialTypeConverter: ProtoConverter[UpdateCredentialTypeRequest, UpdateCredentialType] =
    (request: UpdateCredentialTypeRequest, _) => {
      for {
        updateCredentialType <-
          request.credentialType
            .toRight(new IllegalArgumentException("Empty credentialType field"))
            .toTry
        credentialTypeId <- CredentialTypeId.from(updateCredentialType.id)
        updateCredentialTypeTransformed <- Try {
          updateCredentialType
            .into[UpdateCredentialType]
            .withFieldConst(_.id, credentialTypeId)
            .transform
        }
      } yield updateCredentialTypeTransformed
    }

  implicit val markAsReadyConverter: ProtoConverter[
    MarkAsReadyCredentialTypeRequest,
    MarkAsReadyCredentialType
  ] =
    (request: MarkAsReadyCredentialTypeRequest, _) => {
      CredentialTypeId
        .from(request.credentialTypeId)
        .map(MarkAsReadyCredentialType)
    }

  implicit val markAsArchivedConverter: ProtoConverter[
    MarkAsArchivedCredentialTypeRequest,
    MarkAsArchivedCredentialType
  ] =
    (request: MarkAsArchivedCredentialTypeRequest, _) => {
      CredentialTypeId
        .from(request.credentialTypeId)
        .map(MarkAsArchivedCredentialType)
    }

  implicit val participantProfileConverter: ProtoConverter[
    ConsoleUpdateProfileRequest,
    UpdateParticipantProfile
  ] = { (request, _) =>
    {
      for {
        name <- Try {
          if (request.name.trim.isEmpty)
            throw new RuntimeException("The name is required")
          else request.name.trim
        }

        logo <- Try {
          val bytes = request.logo.toByteArray
          if (bytes.isEmpty) None
          else Some(ParticipantLogo(bytes.toVector))
        }
      } yield UpdateParticipantProfile(name, logo)
    }
  }
}
