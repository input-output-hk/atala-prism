package io.iohk.atala.cvp.webextension.background

import cats.syntax.functor._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.models.Command.{
  ApprovalRequestResult,
  GotCredentialRequestsRequiringManualApproval,
  GotRequestsRequiringManualApproval,
  GotRevocationRequestsRequiringManualApproval,
  SignedConnectorResponse,
  OperationInfo,
  WalletStatusResult
}
import io.iohk.atala.cvp.webextension.background.models.{Command, CommandWithResponse, Event}
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, PendingRequest, Role, UserDetails}

import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.util.{Failure, Success, Try}

/**
  * There are some APIs that can be accessed only from the background runner, like http/storage/notifications/etc.
  *
  * A way to call these ones from other contexts is to send a message to the background.
  *
  * A request/response mechanism can be simulated by using promises/futures.
  *
  * Any operation is encoded as JSON and parsed on the background context, which runs the actual operation
  * and returns the result as another message.
  *
  * The BackgroundAPI abstracts all that complex logic from the caller and gives a simple API based on futures.
  */
class BackgroundAPI()(implicit ec: ExecutionContext) {
  def sendBrowserNotification(title: String, message: String): Future[Unit] = {
    val command = Command.SendBrowserNotification(title, message)
    process(command).collect {
      case Event.CommandRejected(reason) =>
        sendBrowserNotification("ERROR", reason) // TODO: Remove hack and let the caller handle errors
        throw new RuntimeException(reason)
      case _: Event.BrowserNotificationSent => ()
    }
  }

  def getSignatureRequests(): Future[GotRequestsRequiringManualApproval] = {
    process(Command.GetRequestsRequiringManualApproval)
  }

  def getRevocationRequests(): Future[GotRevocationRequestsRequiringManualApproval] = {
    process(Command.GetRevocationRequestsRequiringManualApproval)
  }

  def getCredentialSignatureRequests(): Future[GotCredentialRequestsRequiringManualApproval] = {
    process(Command.GetCredentialRequestsRequiringManualApproval)
  }

  def getWalletStatus(): Future[WalletStatusResult] = {
    process(Command.GetWalletStatus)
  }

  def getOperationInfo(): Future[OperationInfo] = {
    process(Command.GetOperationInfo)
  }

  def login(): Future[UserDetails] = {
    process(Command.GetUserSession)
  }

  def enqueueRequestApproval(sessionId: String, request: PendingRequest): Future[ApprovalRequestResult] = {
    process(Command.EnqueueRequestApproval(sessionId, request))
  }

  def signConnectorRequest(
      sessionId: String,
      request: ConnectorRequest,
      nonce: Option[Array[Byte]]
  ): Future[SignedConnectorResponse] = {
    process(Command.SignConnectorRequest(sessionId, request, nonce))
  }

  def verifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String,
      encodedMerkleProof: String
  ): Future[Command.VerifySignedCredentialResponse] = {
    process(Command.VerifySignedCredential(sessionId, signedCredentialStringRepresentation, encodedMerkleProof))
  }

  def approveAllCredentialRequests(): Future[Unit] = {
    process(Command.ApproveAllCredentialRequests)
  }

  def rejectAllCredentialRequests(): Future[Unit] = {
    process(Command.RejectAllCredentialRequests)
  }

  def approvePendingRequest(requestId: Int): Future[Unit] = {
    process(Command.ApprovePendingRequest(requestId))
  }

  def credentialRejectionApproved(requestId: Int): Future[Unit] = {
    process(Command.ApprovePendingRequest(requestId))
  }

  def rejectRequest(requestId: Int): Future[Unit] = {
    process(Command.RejectPendingRequest(requestId))
  }

  def recoverWallet(
      password: String,
      mnemonic: Mnemonic
  ): Future[Unit] = {
    process(Command.RecoverWallet(password, mnemonic)).void
  }

  def createWallet(
      password: String,
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[Unit] = {
    process(Command.CreateWallet(password, mnemonic, role, organisationName, logo)).void
  }

  def unlockWallet(password: String): Future[Unit] = {
    process(Command.UnlockWallet(password))
  }

  def lockWallet(): Future[Unit] = {
    process(Command.LockWallet())
  }

  private def process[Resp](command: CommandWithResponse[Resp])(implicit @nowarn dec: Decoder[Resp]): Future[Resp] = {
    val promise = Promise[Resp]()

    val callback: js.Function1[js.Object, Unit] = { rawResult =>
      log(s"Received response $rawResult")
      val resultTry = Try(rawResult.asInstanceOf[String])
        .flatMap(parse(_).toTry.flatMap(_.as[Either[String, Resp]].toTry))
        .flatMap {
          case Right(response) => Success(response)
          case Left(errorMessage) => Failure(new RuntimeException(errorMessage))
        }
      promise.complete(resultTry)
    }

    val message = (command: Command).asJson.noSpaces
    log(s"Sending command $message")
    // No other extension can see this message, from the official docs:
    // - If the extensionId is omitted, the message will be sent to your own extension.
    chrome.runtime.Runtime
      .sendMessage(message = message, responseCallback = callback)
    promise.future
  }

  private def log(msg: String): Unit = {
    println(s"BackgroundAPI: $msg")
  }
}
