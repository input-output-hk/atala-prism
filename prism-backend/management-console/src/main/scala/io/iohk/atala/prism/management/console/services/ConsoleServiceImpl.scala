package io.iohk.atala.prism.management.console.services

import io.circe.Json
import io.iohk.atala.prism.errors.LoggingContext
import io.iohk.atala.prism.management.console.ManagementConsoleAuthenticator
import io.iohk.atala.prism.management.console.errors.{
  InternalServerError,
  ManagementConsoleError,
  ManagementConsoleErrorSupport
}
import io.iohk.atala.prism.management.console.grpc.ProtoCodecs
import io.iohk.atala.prism.management.console.models.{Contact, CreateContact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  CredentialIssuancesRepository,
  InstitutionGroupsRepository,
  StatisticsRepository
}
import io.iohk.atala.prism.protos.common_models.{HealthCheckRequest, HealthCheckResponse}
import io.iohk.atala.prism.protos.connector_api.{ConnectionsStatusRequest, ContactConnectionServiceGrpc}
import io.iohk.atala.prism.protos.console_api._
import io.iohk.atala.prism.protos.console_models.ContactConnectionStatus
import io.iohk.atala.prism.protos.{connector_models, console_api}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.scalaland.chimney.dsl._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.util.Try

class ConsoleServiceImpl(
    contactsRepository: ContactsRepository,
    statisticsRepository: StatisticsRepository,
    institutionGroupsRepository: InstitutionGroupsRepository,
    credentialIssuancesRepository: CredentialIssuancesRepository,
    authenticator: ManagementConsoleAuthenticator,
    contactConnectionService: ContactConnectionServiceGrpc.ContactConnectionService
)(implicit
    ec: ExecutionContext
) extends ConsoleServiceGrpc.ConsoleService
    with ManagementConsoleErrorSupport {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def healthCheck(request: HealthCheckRequest): Future[HealthCheckResponse] =
    Future.successful(HealthCheckResponse())

  override def createContact(request: CreateContactRequest): Future[CreateContactResponse] = {
    def f(participantId: ParticipantId): Future[CreateContactResponse] = {
      val externalIdF = Future.fromTry {
        Try {
          if (request.externalId.trim.isEmpty) throw new RuntimeException("externalId cannot be empty")
          else Contact.ExternalId(request.externalId.trim)
        }
      }
      lazy val jsonF = Future.fromTry {
        Try {
          val jsonData = Option(request.jsonData).filter(_.nonEmpty).getOrElse("{}")
          io.circe.parser
            .parse(jsonData)
            .getOrElse(throw new RuntimeException("Invalid jsonData: it must be a JSON string"))
        }
      }

      val maybeGroupName =
        if (request.groupName.trim.isEmpty) None else Some(InstitutionGroup.Name(request.groupName.trim))
      for {
        json <- jsonF
        externalId <- externalIdF
        model =
          request
            .into[CreateContact]
            .withFieldConst(_.createdBy, participantId)
            .withFieldConst(_.data, json)
            .withFieldConst(_.externalId, externalId)
            .enableUnsafeOption
            .transform
        loggingContext = LoggingContext("request" -> request, "json" -> json, "model" -> model)
        reponse <-
          contactsRepository
            .create(model, maybeGroupName)
            .map(contact =>
              ProtoCodecs.toContactProto(
                contact,
                connector_models.ContactConnection(
                  connectionStatus = ContactConnectionStatus.INVITATION_MISSING
                )
              )
            )
            .map(console_api.CreateContactResponse().withContact)
            .wrapExceptions(implicitly, loggingContext)
            .flatten
      } yield reponse
    }

    authenticator.authenticated("createContact", request) { participantId =>
      f(participantId)
    }
  }

  override def getContacts(request: GetContactsRequest): Future[GetContactsResponse] = {
    def f(participantId: ParticipantId): Future[GetContactsResponse] = {
      val lastSeenContact = Try(Contact.Id(UUID.fromString(request.lastSeenContactId))).toOption
      val groupName = Option(request.groupName.trim).filter(_.nonEmpty).map(InstitutionGroup.Name.apply)

      implicit val loggingContext: LoggingContext =
        LoggingContext(
          "request" -> request,
          "participantId" -> participantId,
          "lastSeenContact" -> lastSeenContact,
          "groupName" -> groupName
        )

      contactsRepository
        .getBy(participantId, lastSeenContact, groupName, request.limit)
        .flatMap { list =>
          contactConnectionService
            .getConnectionStatus(
              ConnectionsStatusRequest(acceptorIds = list.map(_.contactId.value.toString))
            )
            .map { connectionStatusResponse =>
              Right(
                console_api.GetContactsResponse(
                  list.zip(connectionStatusResponse.connections).map {
                    case (contact, connection) =>
                      ProtoCodecs.toContactProto(contact, connection)
                  }
                )
              )
            }
            .toFutureEither
        }
        .wrapExceptions
        .flatten
    }

    authenticator.authenticated("getSubjects", request) { participantId =>
      f(participantId)
    }
  }

  override def getContact(request: GetContactRequest): Future[GetContactResponse] = {
    def f(participantId: ParticipantId): Future[GetContactResponse] = {
      val contactIdF = Future.fromTry {
        Try {
          Contact.Id(UUID.fromString(request.contactId))
        }
      }

      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "institutionId" -> participantId)

      for {
        contactId <- contactIdF
        response <-
          contactsRepository
            .find(participantId, contactId)
            .flatMap { maybeContact =>
              contactConnectionService
                .getConnectionStatus(
                  ConnectionsStatusRequest(acceptorIds = List(contactId.value.toString))
                )
                .map { connectionStatusResponse =>
                  Right(
                    console_api.GetContactResponse(
                      maybeContact.zip(connectionStatusResponse.connections.headOption).map {
                        case (contact, connection) =>
                          ProtoCodecs.toContactProto(contact, connection)
                      }
                    )
                  )
                }
                .toFutureEither
            }
            .wrapExceptions
            .flatten
      } yield response
    }

    authenticator.authenticated("getSubject", request) { participantId =>
      f(participantId)
    }
  }

  override def generateConnectionTokenForContact(
      request: GenerateConnectionTokenForContactRequest
  ): Future[GenerateConnectionTokenForContactResponse] = ???

  override def createCredentialIssuance(
      request: CreateCredentialIssuanceRequest
  ): Future[CreateCredentialIssuanceResponse] = {
    def asCreateCredentialIssuanceContact(
        institutionId: ParticipantId,
        request: CreateCredentialIssuanceRequest
    ): FutureEither[ManagementConsoleError, List[CredentialIssuancesRepository.CreateCredentialIssuanceContact]] = {
      val contactIdsF = Future {
        Try {
          request.credentialIssuanceContacts.map(contact => Contact.Id(UUID.fromString(contact.contactId)))
        }.toEither
      }.toFutureEither

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
            .map((groupId: String) => InstitutionGroup.Id(UUID.fromString(groupId)))
            .asJava
        _ =
          if (!validGroupIds.containsAll(requestGroupIds)) throw new IllegalArgumentException("Some groups are invalid")
        // Map the contacts so they are ready to be inserted in the DB
        createCredentialIssuanceContacts = request.credentialIssuanceContacts.map { contact =>
          CredentialIssuancesRepository
            .CreateCredentialIssuanceContact(
              contactId = Contact.Id(UUID.fromString(contact.contactId)),
              credentialData = Json.fromString(contact.credentialData),
              groupIds = contact.groupIds.map(groupId => InstitutionGroup.Id(UUID.fromString(groupId))).toList
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
                credentialTypeId = request.credentialTypeId,
                contacts = contacts
              )
            )
            .map { credentialIssuanceId =>
              CreateCredentialIssuanceResponse(credentialIssuanceId = credentialIssuanceId.uuid.toString)
            }
      } yield response

      responseF.wrapExceptions.flatten
    }

    authenticator.authenticated("createCredentialIssuance", request) { institutionId =>
      f(institutionId, request)
    }
  }

  override def getStatistics(request: GetStatisticsRequest): Future[GetStatisticsResponse] = {
    def f(participantId: ParticipantId): Future[GetStatisticsResponse] = {
      implicit val loggingContext: LoggingContext =
        LoggingContext("request" -> request, "participantId" -> participantId)

      for {
        response <-
          statisticsRepository
            .query(participantId)
            .map(ProtoCodecs.toStatisticsProto)
            .wrapExceptions
            .flatten
      } yield response
    }

    authenticator.authenticated("getStatistics", request) { participantId =>
      f(participantId)
    }
  }
}
