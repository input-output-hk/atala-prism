package io.iohk.atala.mirror

import io.iohk.atala.prism.protos.mirror_api.{CreateAccountRequest, CreateAccountResponse, MirrorServiceGrpc}

import scala.concurrent.Future

class MirrorService extends MirrorServiceGrpc.MirrorService {
  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] = ???
}
