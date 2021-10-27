package io.iohk.atala.prism.management.console.repositories

import cats.{Comonad, Functor, Monad}
import cats.data.EitherT
import cats.data.Validated.{Invalid, Valid}
import cats.effect.{BracketThrow, Resource}
import cats.implicits._
import derevo.tagless.applyK
import derevo.derive
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Json
import io.iohk.atala.prism.management.console.errors._
import io.iohk.atala.prism.management.console.models._
import io.iohk.atala.prism.management.console.repositories.daos.CredentialIssuancesDAO.CreateCredentialIssuanceContactGroup
import io.iohk.atala.prism.management.console.repositories.daos._
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.scalaland.chimney.dsl._
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.management.console.models.CredentialTypeWithRequiredFields
import io.iohk.atala.prism.management.console.repositories.CredentialIssuancesRepository.{
  CreateCredentialBulk,
  CreateCredentialIssuance
}
import io.iohk.atala.prism.management.console.repositories.logs.CredentialIssuancesRepositoryLogs
import io.iohk.atala.prism.management.console.repositories.metrics.CredentialIssuancesRepositoryMetrics
import io.iohk.atala.prism.management.console.validations.CredentialDataValidator
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps

@derive(applyK)
trait CredentialIssuancesRepository[F[_]] {

  def create(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]]

  def createBulk(
      participantId: ParticipantId,
      credentialsType: CredentialTypeId,
      issuanceName: String,
      drafts: List[CreateCredentialBulk.Draft]
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]]

  def get(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): F[CredentialIssuance]

}

object CredentialIssuancesRepository {

  case class CreateCredentialIssuance(
      name: String,
      credentialTypeId: CredentialTypeId,
      contacts: List[CreateCredentialIssuanceContact]
  )

  case class CreateCredentialIssuanceContact(
      contactId: Contact.Id,
      credentialData: Json,
      groupIds: List[InstitutionGroup.Id]
  )

  case class GetCredentialIssuance(
      credentialIssuanceId: CredentialIssuance.Id
  )

  case class CreateCredentialBulk(
      credentialsJson: Json,
      drafts: List[CreateCredentialBulk.Draft],
      credentialsType: CredentialTypeId,
      issuanceName: String
  )

  object CreateCredentialBulk {
    case class Draft(
        externalId: Contact.ExternalId,
        credentialData: Json,
        groupIds: Set[InstitutionGroup.Id]
    )
  }

  def apply[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[CredentialIssuancesRepository[F]] =
    for {
      serviceLogs <- logs.service[CredentialIssuancesRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, CredentialIssuancesRepository[F]] = serviceLogs
      val metrics: CredentialIssuancesRepository[Mid[F, *]] =
        new CredentialIssuancesRepositoryMetrics[F]
      val logs: CredentialIssuancesRepository[Mid[F, *]] =
        new CredentialIssuancesRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new CredentialIssuancesRepositoryImpl[F](transactor)
    }

  def unsafe[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): CredentialIssuancesRepository[F] =
    CredentialIssuancesRepository(transactor, logs).extract

  def makeResource[F[_]: TimeMeasureMetric: BracketThrow, R[_]: Monad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, CredentialIssuancesRepository[F]] =
    Resource.eval(
      CredentialIssuancesRepository(transactor, logs)
    )

}

private final class CredentialIssuancesRepositoryImpl[F[_]: BracketThrow](
    xa: Transactor[F]
) extends CredentialIssuancesRepository[F] {
  import CredentialIssuancesRepository._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private def createContacts(
      createCredentialIssuance: CreateCredentialIssuance,
      credentialIssuanceId: CredentialIssuance.Id
  ): ConnectionIO[
    List[(CreateCredentialIssuanceContact, CredentialIssuance.ContactId)]
  ] = {
    createCredentialIssuance.contacts.map { contact =>
      CredentialIssuancesDAO
        .createContact(
          contact
            .into[CredentialIssuancesDAO.CreateCredentialIssuanceContact]
            .withFieldConst(_.credentialIssuanceId, credentialIssuanceId)
            .transform
        )
        .map(contact -> _)
    }.sequence
  }

  private def createGroups(
      createCredentialIssuance: CreateCredentialIssuance,
      credentialIssuanceId: CredentialIssuance.Id
  ): ConnectionIO[
    List[(InstitutionGroup.Id, CredentialIssuance.ContactGroupId)]
  ] = {
    createCredentialIssuance.contacts
      .flatten(_.groupIds)
      .distinct
      .map { groupId =>
        CredentialIssuancesDAO
          .createContactGroup(
            CreateCredentialIssuanceContactGroup(
              credentialIssuanceId = credentialIssuanceId,
              contactGroupId = groupId
            )
          )
          .map(groupId -> _)
      }
      .sequence
  }

  private def linkContactsToCredentialIssuance(
      credentialIssuanceId: CredentialIssuance.Id,
      contactsWithIds: List[
        (CreateCredentialIssuanceContact, CredentialIssuance.ContactId)
      ],
      issuanceGroupIdByGroupId: Map[
        InstitutionGroup.Id,
        CredentialIssuance.ContactGroupId
      ]
  ): ConnectionIO[List[Unit]] = {
    contactsWithIds.flatten { case (contact, issuanceContactId) =>
      if (contact.groupIds.isEmpty) {
        // Add contact directly to the credential issuance as it does not belong to any group
        List(
          CredentialIssuancesDAO.addContactToCredentialIssuance(
            issuanceContactId,
            credentialIssuanceId
          )
        )
      } else {
        // Associate the contact to the groups it belongs to, implicitly associating it with the credential
        // issuance
        contact.groupIds.map(groupId =>
          CredentialIssuancesDAO.addContactToGroup(
            issuanceContactId,
            issuanceGroupIdByGroupId(groupId)
          )
        )
      }
    }.sequence
  }

  private def createGenericCredentials(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance,
      contactsWithIds: List[
        (CreateCredentialIssuanceContact, CredentialIssuance.ContactId)
      ]
  ): ConnectionIO[List[GenericCredential]] = {
    contactsWithIds.map { case (contact, issuanceContactId) =>
      CredentialsDAO
        .create(
          participantId,
          contact.contactId,
          CreateGenericCredential(
            credentialData = contact.credentialData,
            credentialIssuanceContactId = Some(issuanceContactId),
            credentialTypeId = createCredentialIssuance.credentialTypeId,
            contactId = None,
            externalId = None
          )
        )
    }.sequence
  }

  private def validateCredentialData(
      credentialTypeWithRequiredFields: CredentialTypeWithRequiredFields,
      createCredentialIssuance: CreateCredentialIssuance
  ): EitherT[ConnectionIO, ManagementConsoleError, Unit] = {
    val contactsWithInvalidCredentialData =
      createCredentialIssuance.contacts.flatMap { contact =>
        CredentialDataValidator.validate(
          credentialTypeWithRequiredFields,
          contact.credentialData
        ) match {
          case Valid(_) => None
          case Invalid(errors) =>
            Some((contact.contactId, contact.credentialData, errors.toList))
        }
      }

    if (contactsWithInvalidCredentialData.nonEmpty) {
      EitherT.fromEither[ConnectionIO](
        Left(
          CredentialDataValidationFailedForContacts(
            credentialTypeWithRequiredFields.credentialType.name,
            contactsWithInvalidCredentialData
          )
        )
      )
    } else {
      EitherT.fromEither[ConnectionIO](Right(()))
    }
  }

  private def validate(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): EitherT[ConnectionIO, ManagementConsoleError, Unit] = {
    val contactIds = createCredentialIssuance.contacts.map(_.contactId)
    for {
      // validate credential data
      credentialTypeWithRequiredFields <- EitherT(
        CredentialTypeDao.findValidated(
          createCredentialIssuance.credentialTypeId,
          participantId
        )
      )
      _ <- validateCredentialData(
        credentialTypeWithRequiredFields,
        createCredentialIssuance
      )
      // Validate contacts
      contacts <- EitherT.right[ManagementConsoleError](
        ContactsDAO.findContacts(participantId, contactIds)
      )
      _ <-
        if (contacts.size != contactIds.size)
          EitherT.leftT[ConnectionIO, Unit](
            ContactIdsWereNotFound(
              contactIds.toSet -- contacts.map(_.contactId)
            )
          )
        else
          EitherT.rightT[ConnectionIO, ManagementConsoleError](())
      // Validate groups
      groups <- EitherT.right[ManagementConsoleError](
        InstitutionGroupsDAO.getBy(participantId)
      )
      validGroupIds = groups.map(_.value.id).toSet
      requestGroupIds = createCredentialIssuance.contacts
        .flatMap(_.groupIds)
        .to(Set)
      _ <-
        if (!requestGroupIds.subsetOf(validGroupIds))
          EitherT.leftT[ConnectionIO, Unit][ManagementConsoleError](
            InvalidGroups(requestGroupIds -- validGroupIds)
          )
        else
          EitherT.rightT[ConnectionIO, ManagementConsoleError](())
    } yield ()
  }

  private def createQuery(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): ConnectionIO[Either[ManagementConsoleError, CredentialIssuance.Id]] = {
    validate(participantId, createCredentialIssuance).value.flatMap {
      case Left(error) =>
        error.asLeft[CredentialIssuance.Id].pure[ConnectionIO]
      case Right(_) =>
        for {
          // Create the credential issuance
          credentialIssuanceId <- CredentialIssuancesDAO
            .createCredentialIssuance(
              createCredentialIssuance
                .into[CredentialIssuancesDAO.CreateCredentialIssuance]
                .transform,
              participantId
            )
          // Create the contacts (not associated to the issuance yet)
          contactsWithIds <- createContacts(
            createCredentialIssuance,
            credentialIssuanceId
          )
          // Create the groups the credential issuance was created from
          groupIds <- createGroups(
            createCredentialIssuance,
            credentialIssuanceId
          )
          // Map the group IDs so we can add contacts to them
          issuanceGroupIdByGroupId = groupIds.toMap
          // Associate each contact with the groups it belongs to, or add it to the credential issuance directly otherwise
          _ <- linkContactsToCredentialIssuance(
            credentialIssuanceId,
            contactsWithIds,
            issuanceGroupIdByGroupId
          )
          // Create the credentials
          _ <- createGenericCredentials(
            participantId,
            createCredentialIssuance,
            contactsWithIds
          )
        } yield credentialIssuanceId.asRight[ManagementConsoleError]
    }
  }

  def create(
      participantId: ParticipantId,
      createCredentialIssuance: CreateCredentialIssuance
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]] =
    createQuery(participantId, createCredentialIssuance)
      .logSQLErrors(
        s"creating credential issuance, participant id - $participantId",
        logger
      )
      .transact(xa)

  def createBulk(
      participantId: ParticipantId,
      credentialsType: CredentialTypeId,
      issuanceName: String,
      drafts: List[CreateCredentialBulk.Draft]
  ): F[Either[ManagementConsoleError, CredentialIssuance.Id]] = {
    val validateDraftsF = {
      drafts.map { draft =>
        for {
          contact <- EitherT.fromOptionF(
            ContactsDAO.findContact(participantId, draft.externalId),
            ExternalIdsWereNotFound(
              Set(draft.externalId)
            ): ManagementConsoleError
          )
          // Validate groups
          groups <- EitherT.right[ManagementConsoleError](
            InstitutionGroupsDAO.getBy(participantId)
          )
          validGroupIds = groups.map(_.value.id).toSet
          _ <-
            if (draft.groupIds.exists(x => !validGroupIds.contains(x)))
              EitherT.leftT[ConnectionIO, Unit][ManagementConsoleError](
                InvalidGroups(draft.groupIds -- validGroupIds)
              )
            else
              EitherT.rightT[ConnectionIO, ManagementConsoleError](())
        } yield CredentialIssuancesRepository
          .CreateCredentialIssuanceContact(
            contactId = contact.contactId,
            credentialData = draft.credentialData,
            groupIds = draft.groupIds.toList
          )
      }.sequence
    }

    val query = for {
      contactsEntries <- validateDraftsF
      createCredentialIssuance = CreateCredentialIssuance(
        name = issuanceName,
        credentialTypeId = credentialsType,
        contacts = contactsEntries
      )
      credentialId <- EitherT(
        createQuery(participantId, createCredentialIssuance)
      )
    } yield credentialId

    query.value
      .logSQLErrors(
        s"creating bulk credential issuance, participant id - $participantId",
        logger
      )
      .transact(xa)
  }

  def get(
      credentialIssuanceId: CredentialIssuance.Id,
      institutionId: ParticipantId
  ): F[CredentialIssuance] = {
    val query = for {
      // Get the issuance first, as contacts are queried later
      issuanceWithoutContacts <-
        CredentialIssuancesDAO.getCredentialIssuanceWithoutContacts(
          credentialIssuanceId,
          institutionId
        )
      // Get the contacts without contacts, as they are queried later
      contactsWithoutGroups <- CredentialIssuancesDAO.listContactsWithoutGroups(
        credentialIssuanceId
      )
      // Determine which contacts belong to which group
      groupsPerContactList <- CredentialIssuancesDAO.listGroupsPerContact(
        credentialIssuanceId
      )
      groupsPerContact = groupsPerContactList
        .groupMap(_._1)(_._2)
        .withDefaultValue(List())
      contacts = contactsWithoutGroups.map { contact =>
        contact
          .into[CredentialIssuanceContact]
          .withFieldConst(_.groupIds, groupsPerContact(contact.id))
          .transform
      }
      issuance = issuanceWithoutContacts
        .into[CredentialIssuance]
        .withFieldConst(_.contacts, contacts)
        .transform
    } yield issuance

    query
      .logSQLErrors(
        s"getting credential, issuance id - $credentialIssuanceId",
        logger
      )
      .transact(xa)
  }
}
