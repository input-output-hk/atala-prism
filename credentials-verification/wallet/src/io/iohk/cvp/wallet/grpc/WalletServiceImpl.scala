package io.iohk.cvp.wallet.grpc

import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos
import io.iohk.cvp.wallet.WalletServiceOrchestrator._
import io.iohk.cvp.wallet.protos._
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import io.iohk.cvp.wallet.WalletSecurity._
import io.iohk.cvp.wallet.protos.GetWalletStatusResponse.WalletStatus

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val walletFile = os.pwd / ".cvpwallet" / "wallet.dat"

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

  override def createWallet(request: CreateWalletRequest): Future[CreateWalletResponse] = {
    createNewWallet(request.passphrase).map { _ =>
      CreateWalletResponse()
    }
  }

  override def getWalletStatus(request: GetWalletStatusRequest): Future[GetWalletStatusResponse] = {
    loadWallet().map {
      case Some(_) => GetWalletStatusResponse(WalletStatus.Locked)
      case None => GetWalletStatusResponse(WalletStatus.Missing)
    }
  }

}
