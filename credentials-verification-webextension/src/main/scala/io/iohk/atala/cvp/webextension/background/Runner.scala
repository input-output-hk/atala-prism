package io.iohk.atala.cvp.webextension.background

import io.circe.generic.auto._
import io.circe.{Encoder, Json}
import io.iohk.atala.cvp.webextension.Config
import io.iohk.atala.cvp.webextension.background.models.Command
import io.iohk.atala.cvp.webextension.background.services.browser.{
  BrowserActionService,
  BrowserNotificationService,
  BrowserTabService
}
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager
import io.iohk.atala.cvp.webextension.common.I18NMessages

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

final case class CommandResponse[T](response: T)(implicit val enc: Encoder[T])

private object Logger {
  def log(msg: String): Unit = {
    println(s"background: $msg")
  }
}

class Runner(
    commandProcessor: CommandProcessor
)(implicit ec: ExecutionContext) {

  implicit val encodeNothing = new Encoder[Nothing] {
    override def apply(a: Nothing): Json = ???
  }

  def run(): Unit = {
    Logger.log("This was run by the background script")
    processExternalMessages()
  }

  // this is needed as defining a generic method seems to be the only way to tell Scala
  // that in response: CommandResponse[_] encoder response.enc is the right one for response.response
  private def encodeCommandResponseAsRight[T](response: CommandResponse[T]): Json = {
    implicit val responseEnc: Encoder[T] = response.enc
    implicitly[Encoder[Either[Nothing, T]]].apply(Right(response.response))
  }

  /**
    * Enables the future-based communication between contexts to the background contexts.
    *
    * Internally, this is done by string-based messages, which we encode as JSON.
    */
  private def processExternalMessages(): Unit = {
    // The messages are expected from:
    // - Our extension (like the browser action icon).
    // - Websites where our content-script was injected.
    //
    // Any other sender is ignored.
    //
    // As the extension id matches, there is no need to check the sender url as
    // it's one of the urls where our content-script gets injected.
    val myExtensionId = chrome.runtime.Runtime.id
    chrome.runtime.Runtime.onMessage
      .filter { message =>
        // Ignore messages from anyone else
        //
        // NOTE: While testing with selenium, the extension id is "id", which allow us to get the test passing
        //       without disabling the id check.
        message.sender.id.getOrElse("id") == myExtensionId
      }
      .listen { message =>
        message.value.foreach { any =>
          implicit lazy val origin: String = message.sender.url.toOption
            .getOrElse(throw new RuntimeException("Origin of the request Unknown"))
          val response = Future
            .fromTry { Try(any.asInstanceOf[String]).flatMap(Command.decode) }
            .map { cmd =>
              Logger.log(s"Got command = $cmd")
              cmd
            }
            .flatMap(commandProcessor.process)
            .map(encodeCommandResponseAsRight(_))
            .transformWith {
              case Success(response: Json) =>
                Logger.log(s"Responding successfully: ${response.toString}")
                Future.successful(response)
              case Failure(NonFatal(ex)) =>
                Logger.log(s"Failed to process command, error = ${ex.getMessage}")
                Future.successful {
                  implicitly[Encoder[Either[String, Nothing]]]
                    .apply(Left(ex.getMessage))
                }
              case Failure(ex) =>
                Logger.log(s"Impossible failure: ${ex.getMessage}")
                Future.failed(ex)
            }
            .map(_.noSpaces)

          /**
            * NOTE: When replying on futures, the method returning an async response is the only reliable one
            * otherwise, the sender is getting no response, a way to use the async method is to pass a response
            * in case of failures even if that case was already handled with the CommandRejected event.
            */
          message.response(response, "Impossible failure")
        }
      }
  }
}

object Runner {

  def apply(config: Config)(implicit ec: ExecutionContext): Runner = {
    val storage = new StorageService
    val messages = new I18NMessages
    val browserNotificationService = new BrowserNotificationService(messages)
    val browserActionService = new BrowserActionService
    val tabActionService = new BrowserTabService
    val connectorClientService = ConnectorClientService(config.connectorUrl)
    val walletManager =
      new WalletManager(browserActionService, storage, connectorClientService, tabActionService)

    val commandProcessor =
      new CommandProcessor(browserNotificationService, browserActionService, walletManager)

    new Runner(commandProcessor)
  }

  def apply(config: Config, connectorClientService: ConnectorClientService)(implicit ec: ExecutionContext): Runner = {
    val storage = new StorageService
    val messages = new I18NMessages
    val browserNotificationService = new BrowserNotificationService(messages)
    val browserActionService = new BrowserActionService
    val tabActionService = new BrowserTabService

    val walletManager =
      new WalletManager(browserActionService, storage, connectorClientService, tabActionService)

    val commandProcessor =
      new CommandProcessor(browserNotificationService, browserActionService, walletManager)

    new Runner(commandProcessor)
  }
}
