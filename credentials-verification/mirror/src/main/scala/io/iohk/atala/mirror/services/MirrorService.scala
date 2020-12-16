package io.iohk.atala.mirror.services

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

class MirrorService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  def createAccount: Task[CreateAccountResponse] = {
    connectorService.generateConnectionToken
      .flatMap(response => {
        val newToken = ConnectionToken(response.token)

        ConnectionDao
          .insert(Connection(newToken, None, ConnectionState.Invited, None, None))
          .transact(tx)
          .map(_ => CreateAccountResponse(newToken.token))
      })
  }

  def getCredentialForAddress(request: GetCredentialForAddressRequest): Task[GetCredentialForAddressResponse] = {
    val credentialsOption = (for {
      address <- OptionT(CardanoAddressInfoDao.findBy(CardanoAddress(request.address)))
      credentials <- OptionT.liftF(UserCredentialDao.findBy(address.connectionToken))
    } yield credentials).transact(tx).value

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

    } yield redlandCredential).transact(tx).value

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

    Person(Person.Person.NaturalPerson(naturalPerson))
  }
}
