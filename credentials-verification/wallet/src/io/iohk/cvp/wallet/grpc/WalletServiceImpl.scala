package io.iohk.cvp.wallet.grpc

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.wallet.ECKeyOperation._
import io.iohk.cvp.wallet._
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos.GetWalletStatusResponse.WalletStatus
import io.iohk.cvp.wallet.protos.{ChangePassphraseResponse, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {
  private val cachedWallet: AtomicReference[Option[WalletData]] = new AtomicReference(None)
  private val walletSecurity = WalletSecurity()
  private val walletIO = WalletIO()

  private val walletServiceOrchestrator = WalletServiceOrchestrator(walletSecurity, walletIO)

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

  override def createWallet(request: CreateWalletRequest): Future[CreateWalletResponse] = {
    walletServiceOrchestrator
      .createNewWallet(request.passphrase, request.role, request.organisationName, request.logo.toByteArray)
      .map { data =>
        cachedWallet.set(Some(data))

        protos.CreateWalletResponse(Some(toSignedAtalaOperation(data.keyPair)))
      }
  }

  override def getWalletStatus(request: GetWalletStatusRequest): Future[GetWalletStatusResponse] = {
    Future {
      cachedWallet.get()
    }.flatMap {
      case Some(_) => Future.successful(GetWalletStatusResponse(WalletStatus.Unlocked))
      case None =>
        walletIO.load().map {
          case Some(_) => GetWalletStatusResponse(WalletStatus.Locked)
          case None => GetWalletStatusResponse(WalletStatus.Missing)
        }

    }

  }

  override def unlockWallet(request: UnlockWalletRequest): Future[UnlockWalletResponse] = {
    val walletFuture = cachedWallet.get() match {
      case Some(wallet) => Future.successful(wallet)
      case None =>
        walletServiceOrchestrator
          .loadWallet(request.passphrase)
          .map(_.getOrElse(throw new RuntimeException("Wallet cannot be Unlocked")))
          .map(protos.WalletData.parseFrom)
          .andThen {
            case Success(wallet) =>
              cachedWallet.compareAndSet(None, Some(wallet))
            case _ => ()
          }
    }

    walletFuture.map { wallet =>
      UnlockWalletResponse()
        .withOrganisationName(wallet.organisationName)
        .withRole(wallet.role)
        .withLogo(wallet.logo)
    }
  }

  override def lockWallet(request: LockWalletRequest): Future[LockWalletResponse] = {
    Future {
      cachedWallet.set(None)
    }.map(_ => LockWalletResponse())
  }

  override def changePassphrase(request: ChangePassphraseRequest): Future[ChangePassphraseResponse] = {
    walletServiceOrchestrator.loadWallet(request.currentPassphrase).flatMap {
      case Some(data) => {
        val walletData = protos.WalletData.parseFrom(data)
        walletServiceOrchestrator.save(request.newPassphrase, walletData).map { _ =>
          cachedWallet.set(Some(walletData))
          ChangePassphraseResponse()
        }
      }
      case None => throw new RuntimeException("Wallet cannot be Unlocked")
    }
  }

  override def generateDID(request: GenerateDIDRequest): Future[GenerateDIDResponse] = {
    Future {
      cachedWallet.get() match {
        case Some(data) => {
          GenerateDIDResponse(Some(toSignedAtalaOperation(data.keyPair)))
        }
        case None => throw new RuntimeException("Wallet is locked")
      }
    }
  }

}
