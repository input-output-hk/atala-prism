package io.iohk.atala.mirror

import scala.concurrent.Future

import monix.eval.Task
import monix.execution.Scheduler
import doobie.util.transactor.Transactor
import doobie.implicits._

import io.iohk.atala.mirror.protos.mirror_api.{CreateAccountRequest, CreateAccountResponse, MirrorServiceGrpc}
import io.iohk.atala.mirror.db.ConnectionDao
import io.iohk.atala.mirror.models.Connection

class MirrorService(tx: Transactor[Task])(implicit s: Scheduler) extends MirrorServiceGrpc.MirrorService {

  /**
    * This is an example of usage, it'll be removed in the future.
    */
  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] =
    ConnectionDao
      .insert(Connection(Connection.ConnectionToken("token"), None, Connection.ConnectionState.Invited))
      .transact(tx)
      .map(_ => CreateAccountResponse("test"))
      .runToFuture
}
