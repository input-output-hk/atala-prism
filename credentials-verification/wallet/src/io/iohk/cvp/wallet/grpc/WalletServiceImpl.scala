package io.iohk.cvp.wallet.grpc

import java.security.PublicKey

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos
import io.iohk.cvp.wallet.protos.{
  SignMessageRequest,
  SignMessageResponse,
  VerifySignedMessageRequest,
  VerifySignedMessageResponse
}

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {
  override def getDID(request: protos.GetDIDRequest): Future[protos.GetDIDResponse] = {
    val response = protos.GetDIDResponse(did = wallet.did)
    Future.successful(response)
  }

  override def signMessage(request: SignMessageRequest): Future[SignMessageResponse] = {
    Future {
      val signature = ECSignature.sign(wallet.privateKey, request.message.toByteArray)
      val response = protos.SignMessageResponse(signature = ByteString.copyFrom(signature.toArray))
      response
    }
  }

  override def verifySignedMessage(request: VerifySignedMessageRequest): Future[VerifySignedMessageResponse] = {
    Future {
      val publicKey = toPublicKey(request.publicKey.getOrElse(throw new RuntimeException("Missing public key")))
      ECSignature.verify(
        publicKey,
        data = request.message.toByteArray,
        signature = request.signature.toByteArray.toVector
      )
      val response = protos.VerifySignedMessageResponse(verified = true)
      response
    }
  }

  private def toPublicKey(proto: protos.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(throw new RuntimeException("Invalid public key"))
  }
}
