package io.iohk.cvp.wallet.grpc

import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {
  override def getDID(request: protos.GetDIDRequest): Future[protos.GetDIDResponse] = {
    val response = protos.GetDIDResponse(did = wallet.did)
    Future.successful(response)
  }
}
