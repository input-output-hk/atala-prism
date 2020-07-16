package io.iohk.atala.cvp.webextension.background

import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.models.Command.{
  KeyList,
  SignedConnectorResponse,
  SigningRequests,
  WalletStatusResult
}
import io.iohk.atala.cvp.webextension.background.models.{Command, CommandWithResponse, Event}
import io.iohk.atala.cvp.webextension.background.wallet.Role
import io.iohk.atala.cvp.webextension.common.Mnemonic
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject, UserDetails}

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

  def createKey(keyName: String): Future[Unit] = {
    process(Command.CreateKey(keyName))
  }

  def listKeys(): Future[KeyList] = {
    process(Command.ListKeys)
  }

  def getSignatureRequests(): Future[SigningRequests] = {
    process(Command.GetSigningRequests)
  }

  def getWalletStatus(): Future[WalletStatusResult] = {
    process(Command.GetWalletStatus)
  }

  def login(): Future[UserDetails] = {
    process(Command.GetUserSession)
  }

  def requestSignature(sessionId: String, subject: CredentialSubject): Future[Unit] = {
    process(Command.RequestSignature(sessionId, subject))
  }

  def signConnectorRequest(sessionId: String, request: ConnectorRequest): Future[SignedConnectorResponse] = {
    process(Command.SignConnectorRequest(sessionId, request))
  }

  def signRequestAndPublish(requestId: Int): Future[Unit] = {
    process(Command.SignRequest(requestId))
  }

  def recoverWallet(
      password: String,
      mnemonic: Mnemonic
  ): Future[Unit] = {
    process(Command.RecoverWallet(password, mnemonic)).map(_ => ())
  }

  def createWallet(
      password: String,
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[Unit] = {
    process(Command.CreateWallet(password, mnemonic, role, organisationName, logo)).map(_ => ())
  }

  def unlockWallet(password: String): Future[Unit] = {
    process(Command.UnlockWallet(password))
  }

  def lockWallet(): Future[Unit] = {
    process(Command.LockWallet())
  }

  private def process[Resp](command: CommandWithResponse[Resp])(implicit dec: Decoder[Resp]): Future[Resp] = {
    val promise = Promise[Resp]

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
