package io.iohk.atala.mirror

import io.iohk.atala.mirror.models.CardanoAddress

import scala.concurrent.Future
import monix.execution.Scheduler
import io.iohk.atala.mirror.protos.mirror_api.{
  CreateAccountRequest,
  CreateAccountResponse,
  GetCredentialForAddressRequest,
  GetCredentialForAddressResponse,
  GetIdentityInfoForAddressRequest,
  GetIdentityInfoForAddressResponse,
  MirrorServiceGrpc
}
import io.iohk.atala.mirror.protos.mirror_models.MirrorError.ADDRESS_NOT_FOUND
import io.iohk.atala.mirror.services.MirrorServiceImpl

class MirrorGrpcService(mirrorService: MirrorServiceImpl)(implicit s: Scheduler)
    extends MirrorServiceGrpc.MirrorService {

  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] = {
    mirrorService.createAccount.runToFuture
  }

  override def getCredentialForAddress(
      request: GetCredentialForAddressRequest
  ): Future[GetCredentialForAddressResponse] = {
    mirrorService.getCredentialForAddress(request).runToFuture
  }

  override def getIdentityInfoForAddress(
      request: GetIdentityInfoForAddressRequest
  ): Future[GetIdentityInfoForAddressResponse] = {
    mirrorService
      .getIdentityInfoForAddress(CardanoAddress(request.address))
      .map {
        case Some(person) =>
          GetIdentityInfoForAddressResponse(
            GetIdentityInfoForAddressResponse.Response.Person(person)
          )

        case None =>
          GetIdentityInfoForAddressResponse(
            GetIdentityInfoForAddressResponse.Response.Error(ADDRESS_NOT_FOUND)
          )
      }
      .runToFuture
  }
}
