package io.iohk.atala.prism.kycbridge.services

import io.iohk.atala.kycbridge.protos.kycbridge_api.{CreateAccountRequest, CreateAccountResponse, KycBridgeServiceGrpc}
import monix.execution.Scheduler

import scala.concurrent.Future

class KycBridgeGrpcService(kycBridgeService: KycBridgeService)(implicit s: Scheduler)
    extends KycBridgeServiceGrpc.KycBridgeService {
  override def createAccount(request: CreateAccountRequest): Future[CreateAccountResponse] = {
    kycBridgeService.createAccount.runToFuture
  }
}
