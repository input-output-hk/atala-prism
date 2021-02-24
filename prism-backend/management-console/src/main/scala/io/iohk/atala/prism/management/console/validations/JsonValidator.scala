package io.iohk.atala.prism.management.console.validations

import io.circe.{Decoder, Json}
import io.iohk.atala.prism.management.console.models.{Contact, InstitutionGroup, ParticipantId}
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  CredentialIssuancesRepository,
  InstitutionGroupsRepository
}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object JsonValidator {
  def parse(string: String): Try[Json] =
    Try {
      io.circe.parser
        .parse(string)
        .getOrElse(throw new RuntimeException("Invalid json: it must be a JSON string"))
    }

  def jsonData(string: String): Try[Json] = {
    val jsonData = Option(string).filter(_.nonEmpty).getOrElse("{}")
    parse(jsonData)
  }

  def jsonDataF(string: String): Future[Json] = {
    Future.fromTry(jsonData(string))
  }

  def extractField[A: Decoder](json: Json)(name: String): Future[A] = {
    json.hcursor
      .downField(name)
      .as[A]
      .fold(
        _ => Future.failed(new RuntimeException(s"Failed to parse $name")),
        Future.successful
      )
  }

  def extractFieldWith[A: Decoder, B](
      json: Json
  )(name: String)(f: A => B)(implicit executionContext: ExecutionContext): Future[B] = {
    extractField(json)(name).map(f)
  }

  def validateIssuanceContacts(
      institutionId: ParticipantId,
      drafts: List[Json],
      contactsRepository: ContactsRepository,
      institutionGroupsRepository: InstitutionGroupsRepository
  )(implicit
      executionContext: ExecutionContext
  ): Future[List[CredentialIssuancesRepository.CreateCredentialIssuanceContact]] = {
    def computeSubjectId(institutionId: ParticipantId, externalId: String): Future[Contact.Id] = {
      Option(externalId)
        .filter(_.nonEmpty)
        .map(getContactId(institutionId))
        .getOrElse(Future.failed(new RuntimeException("Empty externalId")))
    }

    def getContactId(
        institutionId: ParticipantId
    )(externalId: String): Future[Contact.Id] = {
      contactsRepository
        .find(institutionId, Contact.ExternalId(externalId))
        .map(_.getOrElse(throw new RuntimeException("The given externalId doesn't exist")))
        .map(_.contactId)
        .value
        .map {
          case Left(ex) => throw ex
          case Right(contactId) => contactId
        }
    }

    Future.sequence(
      drafts.map { json =>
        for {
          externalId <- JsonValidator.extractField[String](json)("external_id")
          credentialJson <- JsonValidator.extractField[Json](json)("credential_data")
          subjectId <- computeSubjectId(institutionId, externalId)
          // Validate groups
          groups <-
            institutionGroupsRepository
              .getBy(institutionId, None)
              .value
              .map(_.fold(identity, identity))
          validGroupIds = groups.map(_.value.id).toSet
          requestGroupIds =
            json.hcursor
              .downField("group_ids")
              .as[List[String]]
              .getOrElse(List())
              .toSet
              .map((groupId: String) => InstitutionGroup.Id.unsafeFrom(groupId))
          _ = if (requestGroupIds.exists(x => !validGroupIds.contains(x)))
            throw new IllegalArgumentException("Some groups are invalid")
        } yield CredentialIssuancesRepository
          .CreateCredentialIssuanceContact(
            contactId = subjectId,
            credentialData = credentialJson,
            groupIds = requestGroupIds.toList
          )
      }
    )
  }
}
