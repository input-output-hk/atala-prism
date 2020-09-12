package io.iohk.atala.mirror

import scala.concurrent.Future

import monix.eval.Task
import monix.execution.Scheduler
import doobie.util.transactor.Transactor
import doobie.implicits._

import io.iohk.atala.mirror.protos.mirror_api.{CreateAccountRequest, CreateAccountResponse, MirrorServiceGrpc}
import io.iohk.atala.mirror.db.ExampleDao

class MirrorService(tx: Transactor[Task])(implicit s: Scheduler) extends MirrorServiceGrpc.MirrorService {
  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] =
    ExampleDao.test.transact(tx).map(CreateAccountResponse(_)).runToFuture
}
