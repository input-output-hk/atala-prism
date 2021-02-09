package io.iohk.atala.prism.management.console.grpc

import com.google.protobuf.ByteString
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.protos.common_models.SortByDirection
import io.iohk.atala.prism.management.console.models.{Contact, GenericCredential, Statistics}
import io.iohk.atala.prism.protos.{common_models, connector_models, console_api, console_models}
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

import java.time.LocalDate
import scala.util.Try

object ProtoCodecs {

  import PaginatedQueryConstraints._

  implicit val proto2DateTransformer: Transformer[common_models.Date, LocalDate] = proto => {
    LocalDate.of(proto.year, proto.month, proto.day)
  }

  implicit val date2ProtoTransformer: Transformer[LocalDate, common_models.Date] = date => {
    common_models.Date(year = date.getYear, month = date.getMonthValue, day = date.getDayOfMonth)
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
}
