package io.iohk.cvp.wallet.grpc

import java.security.PublicKey
import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, ECSignature}
import io.iohk.cvp.wallet.WalletServiceOrchestrator.{createNewWallet, _}
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.{WalletIO, protos}
import io.iohk.cvp.wallet.protos.GetWalletStatusResponse.WalletStatus
import io.iohk.cvp.wallet.protos._

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {
  private val cachedWallet: AtomicReference[Option[WalletData]] = new AtomicReference(None)

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
    createNewWallet(request.passphrase).map { data =>
      {
        cachedWallet.set(Some(data))
        protos.CreateWalletResponse()
      }
    }
  }

  override def getWalletStatus(request: GetWalletStatusRequest): Future[GetWalletStatusResponse] = {
    Future {
      cachedWallet.get()
    }.flatMap {
      case Some(_) => Future.successful(GetWalletStatusResponse(WalletStatus.Unlocked))
      case None =>
        WalletIO().load().map {
          case Some(_) => GetWalletStatusResponse(WalletStatus.Locked)
          case None => GetWalletStatusResponse(WalletStatus.Missing)
        }

    }

  }

  override def unlockWallet(request: UnlockWalletRequest): Future[UnlockWalletResponse] = {

    Future {
      cachedWallet.get()
    }.flatMap {
      case Some(_) => Future.successful(UnlockWalletResponse())
      case None =>
        loadWallet(request.passphrase).map {
          case Some(data) => {
            cachedWallet.set(Some(protos.WalletData.parseFrom(data)))
            UnlockWalletResponse()
          }
          case None => throw new RuntimeException("Wallet cannot be Unlocked")
        }
    }

  }

  override def lockWallet(request: LockWalletRequest): Future[LockWalletResponse] = {
    Future {
      cachedWallet.set(None)
    }.map(_ => LockWalletResponse())
  }

}
