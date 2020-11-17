package io.iohk.atala.mirror.services

import cats.data.OptionT
import monix.eval.Task
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse
}
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao, UserCredentialDao}
import io.iohk.atala.mirror.models.Connection
import doobie.implicits._
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.mirror.protos.mirror_models.CredentialData.IssuersDidOption
import io.iohk.atala.mirror.protos.mirror_models.GetCredentialForAddressError.ADDRESS_NOT_FOUND
import io.iohk.atala.mirror.protos.mirror_models.{CredentialData, GetCredentialForAddressData}

class MirrorService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  def createAccount: Task[CreateAccountResponse] = {
    connectorService.generateConnectionToken
      .flatMap(response => {
        val newToken = Connection.ConnectionToken(response.token)

        ConnectionDao
          .insert(Connection(newToken, None, Connection.ConnectionState.Invited, None))
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

}
