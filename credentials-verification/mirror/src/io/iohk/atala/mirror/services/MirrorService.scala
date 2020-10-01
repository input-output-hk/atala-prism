package io.iohk.atala.mirror.services

import monix.eval.Task
import doobie.util.transactor.Transactor
import doobie.implicits._

import io.iohk.atala.mirror.protos.mirror_api.CreateAccountResponse
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.mirror.models.Connection

class MirrorService(tx: Transactor[Task], connectorService: ConnectorClientService) {

  def createAccount: Task[CreateAccountResponse] = {
    connectorService.generateConnectionToken
      .flatMap(response => {
        val newToken = Connection.ConnectionToken(response.token)

        ConnectionDao
          .insert(Connection(newToken, None, Connection.ConnectionState.Invited))
          .transact(tx)
          .map(_ => CreateAccountResponse(newToken.token))
      })
  }

}
