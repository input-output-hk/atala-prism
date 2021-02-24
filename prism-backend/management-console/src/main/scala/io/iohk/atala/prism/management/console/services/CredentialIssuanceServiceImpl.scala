package io.iohk.atala.prism.management.console.services

import io.circe.Json
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{
  InternalServerError,
  ManagementConsoleError,
  ManagementConsoleErrorSupport
}
import io.iohk.atala.prism.management.console.models.{
  Contact,
  CredentialIssuance,
  CredentialTypeId,
  InstitutionGroup,
  ParticipantId
}
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  CredentialIssuancesRepository,
  InstitutionGroupsRepository
}
import io.iohk.atala.prism.management.console.validations.JsonValidator
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.CredentialIssuanceContact
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherFOps
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.util.Try

class CredentialIssuanceServiceImpl(
    contactsRepository: ContactsRepository,
    institutionGroupsRepository: InstitutionGroupsRepository,
    credentialIssuancesRepository: CredentialIssuancesRepository,
    authenticator: ManagementConsoleAuthenticator
)(implicit
    ec: ExecutionContext
) extends console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceService
    with ManagementConsoleErrorSupport {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def createCredentialIssuance(
      request: CreateCredentialIssuanceRequest
  ): Future[CreateCredentialIssuanceResponse] = {
    def asCreateCredentialIssuanceContact(
        institutionId: ParticipantId,
        request: CreateCredentialIssuanceRequest
    ): FutureEither[ManagementConsoleError, List[CredentialIssuancesRepository.CreateCredentialIssuanceContact]] = {
      val contactIdsF = FutureEither {
        request.credentialIssuanceContacts.map(contact => Contact.Id.unsafeFrom(contact.contactId))
      }

      val contacts = for {
        // Validate contacts
        contactIds <- contactIdsF
        contacts <- contactsRepository.findContacts(institutionId, contactIds.toList)
        _ = if (contacts.size != contactIds.size) throw new IllegalArgumentException("Some contacts are invalid")
        // Validate groups
        groups <- institutionGroupsRepository.getBy(institutionId, None)
        validGroupIds = groups.map(_.value.id).toSet.asJava
        requestGroupIds =
          request.credentialIssuanceContacts
            .flatten(_.groupIds)
            .toSet
            .map((groupId: String) => InstitutionGroup.Id.unsafeFrom(groupId))
            .asJava
        _ =
          if (!validGroupIds.containsAll(requestGroupIds)) throw new IllegalArgumentException("Some groups are invalid")
        // Map the contacts so they are ready to be inserted in the DB
        createCredentialIssuanceContacts = request.credentialIssuanceContacts.map { contact =>
          CredentialIssuancesRepository
            .CreateCredentialIssuanceContact(
              contactId = Contact.Id.unsafeFrom(contact.contactId),
              credentialData = io.circe.parser
                .parse(contact.credentialData)
                .getOrElse(throw new RuntimeException("Invalid credentialData: it must be a JSON string")),
              groupIds = contact.groupIds.map(groupId => InstitutionGroup.Id.unsafeFrom(groupId)).toList
            )
        }.toList
      } yield createCredentialIssuanceContacts

      contacts.mapLeft(InternalServerError)
    }

    def f(
        institutionId: ParticipantId,
        request: CreateCredentialIssuanceRequest
    ): Future[CreateCredentialIssuanceResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> institutionId)

      val responseF = for {
        contacts <- asCreateCredentialIssuanceContact(institutionId, request)
        response <-
          credentialIssuancesRepository
            .create(
              CredentialIssuancesRepository.CreateCredentialIssuance(
                name = request.name,
                createdBy = institutionId,
                credentialTypeId = CredentialTypeId.unsafeFrom(request.credentialTypeId),
                contacts = contacts
              )
            )
            .map { credentialIssuanceId =>
              CreateCredentialIssuanceResponse(credentialIssuanceId = credentialIssuanceId.toString)
            }
      } yield response

      responseF.wrapExceptions.flatten
    }

    authenticator.authenticated("createCredentialIssuance", request) { institutionId =>
      f(institutionId, request)
    }
  }

  override def getCredentialIssuance(
      request: GetCredentialIssuanceRequest
  ): Future[GetCredentialIssuanceResponse] = {
    def f(
        institutionId: ParticipantId,
        request: GetCredentialIssuanceRequest
    ): Future[GetCredentialIssuanceResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> institutionId)

      val credentialIssuanceIdF = FutureEither {
        CredentialIssuance.Id.from(request.credentialIssuanceId)
      }.mapLeft(InternalServerError)

      val responseF = for {
        credentialIssuanceId <- credentialIssuanceIdF
        credentialIssuance <- credentialIssuancesRepository.get(credentialIssuanceId, institutionId)
        response = GetCredentialIssuanceResponse(
          name = credentialIssuance.name,
          credentialTypeId = credentialIssuance.credentialTypeId.uuid.toString,
          createdAt = credentialIssuance.createdAt.toEpochMilli,
          credentialIssuanceContacts = credentialIssuance.contacts.map(contact =>
            CredentialIssuanceContact(
              contactId = contact.contactId.toString,
              credentialData = contact.credentialData.noSpaces,
              groupIds = contact.groupIds.map(_.toString)
            )
          )
        )
      } yield response

      responseF.wrapExceptions.flatten
    }

    authenticator.authenticated("getCredentialIssuance", request) { institutionId =>
      f(institutionId, request)
    }
  }

  /** Bulk version of CreateGenericCredential, creates credentials on the authenticated institution.
    */
  override def createGenericCredentialBulk(
      request: CreateGenericCredentialBulkRequest
  ): Future[CreateGenericCredentialBulkResponse] =
    authenticator.authenticated[CreateGenericCredentialBulkRequest, CreateGenericCredentialBulkResponse](
      "createGenericCredentialBulk",
      request
    ) { institutionId =>
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> institutionId)

      lazy val credentialsJSONF = Future.fromTry {
        JsonValidator.parse(request.credentialsJSON)
      }

      val contactsEntriesF: Future[List[CredentialIssuancesRepository.CreateCredentialIssuanceContact]] =
        for {
          credentialsJSON <- credentialsJSONF
          drafts <- JsonValidator.extractField[List[Json]](credentialsJSON)("drafts")
          createCredentialIssuanceContacts <- JsonValidator.validateIssuanceContacts(
            institutionId,
            drafts,
            contactsRepository,
            institutionGroupsRepository
          )
        } yield createCredentialIssuanceContacts

      val response = for {
        credentialsJSON <- credentialsJSONF.lift
        credentialsType <-
          JsonValidator
            .extractFieldWith[String, CredentialTypeId](credentialsJSON)("credential_type_id")(
              CredentialTypeId.unsafeFrom
            )
            .lift
        issuanceName <- (JsonValidator.extractField[String](credentialsJSON)("issuance_name"): Future[String]).lift
        _ = Try { if (issuanceName.isEmpty) throw new RuntimeException("Empty issuance name") else () }

        contactsEntries <- contactsEntriesF.lift
        response <-
          credentialIssuancesRepository
            .create(
              CredentialIssuancesRepository.CreateCredentialIssuance(
                name = issuanceName,
                createdBy = institutionId,
                credentialTypeId = credentialsType,
                contacts = contactsEntries
              )
            )
            .map { credentialIssuanceId =>
              CreateGenericCredentialBulkResponse(credentialIssuanceId.uuid.toString)
            }
      } yield response

      response.wrapExceptions.flatten
    }
}
