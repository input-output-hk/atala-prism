package io.iohk.atala.mirror

import scala.concurrent.Future
import monix.execution.Scheduler
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountRequest,
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse,
  MirrorServiceGrpc
}
import io.iohk.atala.mirror.services.MirrorService

class MirrorGrpcService(mirrorService: MirrorService)(implicit s: Scheduler) extends MirrorServiceGrpc.MirrorService {

  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] = {
    mirrorService.createAccount.runToFuture
  }

  override def getCredentialForAddress(
      request: GetCredentialForAddressRequest
  ): Future[GetCredentialForAddressResponse] = {
    mirrorService.getCredentialForAddress(request).runToFuture
  }
}
