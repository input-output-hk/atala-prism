package io.iohk.atala.mirror.services

import java.time.Instant
import cats.data.{NonEmptyList, OptionT}
import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse
}
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.{CardanoAddress, Connection}
import doobie.implicits._
import io.iohk.atala.mirror.protos.ivms101
import io.iohk.atala.mirror.protos.mirror_models.CredentialData.IssuersDidOption
import io.iohk.atala.mirror.protos.mirror_models.MirrorError.ADDRESS_NOT_FOUND
import io.iohk.atala.mirror.protos.mirror_models.{CredentialData, GetCredentialForAddressData}
import cats.implicits._
import doobie.ConnectionIO
import io.iohk.atala.prism.models.{ConnectionState, ConnectionToken}
import io.iohk.atala.prism.services.ConnectorClientService
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import org.slf4j.{Logger, LoggerFactory}

trait MirrorService {
  def createAccount: Task[CreateAccountResponse]

  def getCredentialForAddress(request: GetCredentialForAddressRequest): Task[GetCredentialForAddressResponse]

  def getIdentityInfoForAddress(cardanoAddress: CardanoAddress): Task[Option[ivms101.Person]]
}

class MirrorServiceImpl(tx: Transactor[Task], connectorService: ConnectorClientService) extends MirrorService {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  override def createAccount: Task[CreateAccountResponse] = {
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

  override def getCredentialForAddress(
      request: GetCredentialForAddressRequest
  ): Task[GetCredentialForAddressResponse] = {
    val credentialsOption = (for {
      address <-
        OptionT(CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(request.address))).map(_.headOption))
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

  override def getIdentityInfoForAddress(cardanoAddress: CardanoAddress): Task[Option[ivms101.Person]] = {
    (for {
      address <- OptionT(CardanoAddressInfoDao.findBy(NonEmptyList.of(cardanoAddress)).map(_.headOption))
      credentials <- OptionT.liftF(UserCredentialDao.findBy(address.connectionToken))
      person <-
        credentials
          .sortBy(_.messageReceivedDate.date)
          .flatMap(
            _.toPerson.left
              .map(error => logger.warn(s"Error getting identity info for address: ${error.getMessage}"))
              .toOption
              .flatten
          )
          .lastOption
          .toOptionT[ConnectionIO]
    } yield person).value.logSQLErrors("finding credentials", logger).transact(tx)
  }
}
