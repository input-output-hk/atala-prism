package io.iohk.cvp.wallet.grpc

import java.util.concurrent.atomic.AtomicReference

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECSignature
import io.iohk.cvp.wallet.ECKeyOperation._
import io.iohk.cvp.wallet._
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.prism.protos.{node_models, wallet_api, wallet_internal, wallet_models}

import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class WalletServiceImpl(wallet: Wallet)(implicit ec: ExecutionContext)
    extends wallet_api.WalletServiceGrpc.WalletService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val cachedWallet: AtomicReference[Option[wallet_internal.WalletData]] = new AtomicReference(None)
  private val walletSecurity = WalletSecurity()
  private val walletIO = WalletIO()

  private val walletServiceOrchestrator = WalletServiceOrchestrator(walletSecurity, walletIO)

  override def getDID(request: wallet_api.GetDIDRequest): Future[wallet_api.GetDIDResponse] = {
    logger.info(s"getDID - request = ${request.toProtoString}")
    val response = wallet_api.GetDIDResponse(did = wallet.did)
    logger.info(s"getDID - response = ${response.toProtoString}")
    Future.successful(response)
  }

  override def signMessage(request: wallet_api.SignMessageRequest): Future[wallet_api.SignMessageResponse] = {
    logger.info(s"signMessage - request = ${request.toProtoString}")
    Future {
      val signature = ECSignature.sign(wallet.privateKey, request.message.toByteArray)
      val response = wallet_api.SignMessageResponse(signature = ByteString.copyFrom(signature.toArray))
      logger.info(s"signMessage - response = ${response.toProtoString}")
      response
    }
  }

  override def verifySignedMessage(
      request: wallet_api.VerifySignedMessageRequest
  ): Future[wallet_api.VerifySignedMessageResponse] = {
    logger.info(s"verifySignedMessage - request = ${request.toProtoString}")
    Future {
      val publicKey = toPublicKey(request.publicKey.getOrElse(throw new RuntimeException("Missing public key")))
      ECSignature.verify(
        publicKey,
        data = request.message.toByteArray,
        signature = request.signature.toByteArray.toVector
      )
      val response = wallet_api.VerifySignedMessageResponse(verified = true)
      logger.info(s"verifySignedMessage - response = ${response.toProtoString}")
      response
    }
  }

  override def createWallet(request: wallet_api.CreateWalletRequest): Future[wallet_api.CreateWalletResponse] = {
    logger.info(s"createWallet - request = ${request.toProtoString}")
    walletServiceOrchestrator
      .createNewWallet(request.passphrase, request.role, request.organisationName, request.logo.toByteArray)
      .map { data =>
        cachedWallet.set(Some(data))

        val response = wallet_api.CreateWalletResponse(Some(toSignedAtalaOperation(data.keyPair)))
        logger.info(s"createWallet - response = ${response.toProtoString}")
        response
      }
  }

  override def getWalletStatus(
      request: wallet_api.GetWalletStatusRequest
  ): Future[wallet_api.GetWalletStatusResponse] = {
    logger.info(s"getWalletStatus - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.get()
    }.flatMap {
      case Some(_) =>
        Future.successful(wallet_api.GetWalletStatusResponse(wallet_api.GetWalletStatusResponse.WalletStatus.Unlocked))
      case None =>
        walletIO.load().map {
          case Some(_) => wallet_api.GetWalletStatusResponse(wallet_api.GetWalletStatusResponse.WalletStatus.Locked)
          case None => wallet_api.GetWalletStatusResponse(wallet_api.GetWalletStatusResponse.WalletStatus.Missing)
        }
    }

    result.foreach { response =>
      logger.info(s"getWalletStatus - response = ${response.toProtoString}")
    }

    result
  }

  override def unlockWallet(request: wallet_api.UnlockWalletRequest): Future[wallet_api.UnlockWalletResponse] = {
    logger.info(s"unlockWallet - request = ${request.toProtoString}")
    val walletFuture = cachedWallet.get() match {
      case Some(wallet) => Future.successful(wallet)
      case None =>
        walletServiceOrchestrator
          .loadWallet(request.passphrase)
          .map(_.getOrElse(throw new RuntimeException("Wallet cannot be Unlocked")))
          .map(wallet_internal.WalletData.parseFrom)
          .andThen {
            case Success(wallet) =>
              cachedWallet.compareAndSet(None, Some(wallet))
            case _ => ()
          }
    }

    walletFuture.map { wallet =>
      val response = wallet_api
        .UnlockWalletResponse()
        .withOrganisationName(wallet.organisationName)
        .withRole(wallet.role)
        .withLogo(wallet.logo)

      logger.info(s"unlockWallet - response = ${response.toProtoString}")
      response
    }
  }

  override def lockWallet(request: wallet_api.LockWalletRequest): Future[wallet_api.LockWalletResponse] = {
    logger.info(s"lockWallet - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.set(None)
    }.map(_ => wallet_api.LockWalletResponse())

    result.foreach { response =>
      logger.info(s"lockWallet - response = ${response.toProtoString}")
    }

    result
  }

  override def changePassphrase(
      request: wallet_api.ChangePassphraseRequest
  ): Future[wallet_api.ChangePassphraseResponse] = {
    logger.info(s"changePassphrase - request = ${request.toProtoString}")
    val result = walletServiceOrchestrator.loadWallet(request.currentPassphrase).flatMap {
      case Some(data) =>
        val walletData = wallet_internal.WalletData.parseFrom(data)
        walletServiceOrchestrator.save(request.newPassphrase, walletData).map { _ =>
          cachedWallet.set(Some(walletData))
          wallet_api.ChangePassphraseResponse()
        }

      case None => throw new RuntimeException("Wallet cannot be Unlocked")
    }

    result.foreach { response =>
      logger.info(s"changePassphrase - response = ${response.toProtoString}")
    }

    result
  }

  override def generateDID(request: wallet_api.GenerateDIDRequest): Future[wallet_api.GenerateDIDResponse] = {
    logger.info(s"generateDID - request = ${request.toProtoString}")
    val result = Future {
      cachedWallet.get() match {
        case Some(data) =>
          wallet_api.GenerateDIDResponse(Some(toSignedAtalaOperation(data.keyPair)))

        case None => throw new RuntimeException("Wallet is locked")
      }
    }

    result.foreach { response =>
      logger.info(s"generateDID - response = ${response.toProtoString}")
    }

    result
  }
}
