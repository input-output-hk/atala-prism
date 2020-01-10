package io.iohk.cvp.wallet.grpc

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.wallet.ECKeyOperation._
import io.iohk.cvp.wallet._
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos.GetWalletStatusResponse.WalletStatus
import io.iohk.cvp.wallet.protos.{ChangePassphraseResponse, _}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext) extends protos.WalletServiceGrpc.WalletService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val cachedWallet: AtomicReference[Option[WalletData]] = new AtomicReference(None)
  private val walletSecurity = WalletSecurity()
  private val walletIO = WalletIO()

  private val walletServiceOrchestrator = WalletServiceOrchestrator(walletSecurity, walletIO)

  override def getDID(request: protos.GetDIDRequest): Future[protos.GetDIDResponse] = {
    logger.info(s"getDID - request = ${request.toProtoString}")
    val response = protos.GetDIDResponse(did = wallet.did)
    logger.info(s"getDID - response = ${response.toProtoString}")
    Future.successful(response)
  }

  override def signMessage(request: SignMessageRequest): Future[SignMessageResponse] = {
    logger.info(s"signMessage - request = ${request.toProtoString}")
    Future {
      val signature = ECSignature.sign(wallet.privateKey, request.message.toByteArray)
      val response = protos.SignMessageResponse(signature = ByteString.copyFrom(signature.toArray))
      logger.info(s"signMessage - response = ${response.toProtoString}")
      response
    }
  }

  override def verifySignedMessage(request: VerifySignedMessageRequest): Future[VerifySignedMessageResponse] = {
    logger.info(s"verifySignedMessage - request = ${request.toProtoString}")
    Future {
      val publicKey = toPublicKey(request.publicKey.getOrElse(throw new RuntimeException("Missing public key")))
      ECSignature.verify(
        publicKey,
        data = request.message.toByteArray,
        signature = request.signature.toByteArray.toVector
      )
      val response = protos.VerifySignedMessageResponse(verified = true)
      logger.info(s"verifySignedMessage - response = ${response.toProtoString}")
      response
    }
  }

  override def createWallet(request: CreateWalletRequest): Future[CreateWalletResponse] = {
    logger.info(s"createWallet - request = ${request.toProtoString}")
    walletServiceOrchestrator
      .createNewWallet(request.passphrase, request.role, request.organisationName, request.logo.toByteArray)
      .map { data =>
        cachedWallet.set(Some(data))

        val response = protos.CreateWalletResponse(Some(toSignedAtalaOperation(data.keyPair)))
        logger.info(s"createWallet - response = ${response.toProtoString}")
        response
      }
  }

  override def getWalletStatus(request: GetWalletStatusRequest): Future[GetWalletStatusResponse] = {
    logger.info(s"getWalletStatus - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.get()
    }.flatMap {
      case Some(_) => Future.successful(GetWalletStatusResponse(WalletStatus.Unlocked))
      case None =>
        walletIO.load().map {
          case Some(_) => GetWalletStatusResponse(WalletStatus.Locked)
          case None => GetWalletStatusResponse(WalletStatus.Missing)
        }
    }

    result.foreach { response =>
      logger.info(s"getWalletStatus - response = ${response.toProtoString}")
    }

    result
  }

  override def unlockWallet(request: UnlockWalletRequest): Future[UnlockWalletResponse] = {
    logger.info(s"unlockWallet - request = ${request.toProtoString}")
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
      val response = UnlockWalletResponse()
        .withOrganisationName(wallet.organisationName)
        .withRole(wallet.role)
        .withLogo(wallet.logo)

      logger.info(s"unlockWallet - response = ${response.toProtoString}")
      response
    }
  }

  override def lockWallet(request: LockWalletRequest): Future[LockWalletResponse] = {
    logger.info(s"lockWallet - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.set(None)
    }.map(_ => LockWalletResponse())

    result.foreach { response =>
      logger.info(s"lockWallet - response = ${response.toProtoString}")
    }

    result
  }

  override def changePassphrase(request: ChangePassphraseRequest): Future[ChangePassphraseResponse] = {
    logger.info(s"changePassphrase - request = ${request.toProtoString}")
    val result = walletServiceOrchestrator.loadWallet(request.currentPassphrase).flatMap {
      case Some(data) => {
        val walletData = protos.WalletData.parseFrom(data)
        walletServiceOrchestrator.save(request.newPassphrase, walletData).map { _ =>
          cachedWallet.set(Some(walletData))
          ChangePassphraseResponse()
        }
      }
      case None => throw new RuntimeException("Wallet cannot be Unlocked")
    }

    result.foreach { response =>
      logger.info(s"changePassphrase - response = ${response.toProtoString}")
    }

    result
  }

  override def generateDID(request: GenerateDIDRequest): Future[GenerateDIDResponse] = {
    logger.info(s"generateDID - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.get() match {
        case Some(data) =>
          GenerateDIDResponse(Some(toSignedAtalaOperation(data.keyPair)))

        case None => throw new RuntimeException("Wallet is locked")
      }
    }

    result.foreach { response =>
      logger.info(s"generateDID - response = ${response.toProtoString}")
    }

    result
  }
}
