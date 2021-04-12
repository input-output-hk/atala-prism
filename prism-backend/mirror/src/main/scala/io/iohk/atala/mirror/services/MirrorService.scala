package io.iohk.atala.mirror.services

import java.time.Instant
import cats.data.OptionT
import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse,
  GetIdentityInfoForAddressRequest,
  GetIdentityInfoForAddressResponse
}
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.{Connection, RedlandIdCredential}
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.mirror.protos.ivms101.{
  DateAndPlaceOfBirth,
  NationalIdentification,
  NationalIdentifierTypeCode,
  NaturalPerson,
  NaturalPersonName,
  NaturalPersonNameId,
  NaturalPersonNameTypeCode,
  Person
}
import io.iohk.atala.mirror.protos.mirror_models.CredentialData.IssuersDidOption
import io.iohk.atala.mirror.protos.mirror_models.MirrorError.ADDRESS_NOT_FOUND
import io.iohk.atala.mirror.protos.mirror_models.{CredentialData, GetCredentialForAddressData}
import cats.implicits._
import doobie.ConnectionIO
import io.iohk.atala.prism.credentials.Credential
import io.circe.generic.auto._
import io.iohk.atala.prism.models.{ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

class MirrorService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def createAccount: Task[CreateAccountResponse] = {
    connectorService.generateConnectionToken
      .flatMap(response => {
        response.tokens.headOption
          .map { tokenString =>
            val newToken = ConnectionToken(tokenString)

            ConnectionDao
              .insert(Connection(newToken, None, ConnectionState.Invited, Instant.now(), None, None))
              .logSQLErrors("inserting connection", logger)
              .transact(tx)
              .as(CreateAccountResponse(newToken.token))
          }
          .getOrElse(
            Task.raiseError(
              new RuntimeException("GenerateConnectionToken response returned empty list of connection tokens")
            )
          )
      })
  }

  def getCredentialForAddress(request: GetCredentialForAddressRequest): Task[GetCredentialForAddressResponse] = {
    val credentialsOption = (for {
      address <- OptionT(CardanoAddressInfoDao.findBy(CardanoAddress(request.address)))
      credentials <- OptionT.liftF(UserCredentialDao.findBy(address.connectionToken))
    } yield credentials).value.logSQLErrors("finding credentials", logger).transact(tx)

    credentialsOption.map {
      case Some(credentials) =>
        val parsedCredentials = credentials.map { userCredential =>
          CredentialData(
            rawCredential = userCredential.rawCredential.rawCredential,
            issuersDidOption = userCredential.issuersDID.fold[IssuersDidOption](IssuersDidOption.Empty)(did =>
              IssuersDidOption.IssuersDid(did.value)
            ),
            status = userCredential.status.entryName
          )
        }
        GetCredentialForAddressResponse(
          GetCredentialForAddressResponse.Response.Data(GetCredentialForAddressData(parsedCredentials))
        )

      case None =>
        GetCredentialForAddressResponse(
          GetCredentialForAddressResponse.Response.Error(ADDRESS_NOT_FOUND)
        )
    }

  }

  def getIdentityInfoForAddress(request: GetIdentityInfoForAddressRequest): Task[GetIdentityInfoForAddressResponse] = {
    val redlandCredentialOption = (for {
      address <- OptionT(CardanoAddressInfoDao.findBy(CardanoAddress(request.address)))
      credentials <- OptionT.liftF(UserCredentialDao.findBy(address.connectionToken))
      redlandCredential <-
        credentials
          .sortBy(_.messageReceivedDate.date)
          .flatMap(credential =>
            Credential
              .fromString(credential.rawCredential.rawCredential)
              .toOption
          )
          .flatMap(credential => RedlandIdCredential.fromCredentialContent(credential.content).toOption)
          .lastOption
          .toOptionT[ConnectionIO]

    } yield redlandCredential).value.logSQLErrors("finding credentials", logger).transact(tx)

    redlandCredentialOption.map {
      case Some(redlandIdCredential) =>
        val person = redlandIdCredentialToPerson(redlandIdCredential)

        GetIdentityInfoForAddressResponse(
          GetIdentityInfoForAddressResponse.Response.Person(person)
        )

      case None =>
        GetIdentityInfoForAddressResponse(
          GetIdentityInfoForAddressResponse.Response.Error(ADDRESS_NOT_FOUND)
        )
    }
  }

  private def redlandIdCredentialToPerson(redlandIdCredential: RedlandIdCredential): Person = {

    val naturalPersonName = NaturalPersonName(
      nameIdentifiers = Seq(
        NaturalPersonNameId(
          primaryIdentifier = redlandIdCredential.name,
          nameIdentifierType = NaturalPersonNameTypeCode.NATURAL_PERSON_NAME_TYPE_CODE_LEGL
        )
      )
    )

    val nationalIdentification = NationalIdentification(
      nationalIdentifier = redlandIdCredential.identityNumber,
      nationalIdentifierType = NationalIdentifierTypeCode.NATIONAL_IDENTIFIER_TYPE_CODE_MISC
    )

    val dateAndPlaceOfBirth = DateAndPlaceOfBirth(
      dateOfBirth = redlandIdCredential.dateOfBirth
    )

    val naturalPerson = NaturalPerson(
      name = Some(naturalPersonName),
      geographicAddresses = Seq.empty,
      nationalIdentification = Some(nationalIdentification),
      dateAndPlaceOfBirth = Some(dateAndPlaceOfBirth)
    )

    Person(Person.Internal.NaturalPerson(naturalPerson))
  }
}
