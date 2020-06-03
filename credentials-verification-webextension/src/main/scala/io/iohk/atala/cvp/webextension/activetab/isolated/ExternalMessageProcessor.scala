package io.iohk.atala.cvp.webextension.activetab.isolated

import java.util.UUID

import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event, TaggedModel}
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

private[isolated] class ExternalMessageProcessor(commandProcessor: CommandProcessor)(implicit ec: ExecutionContext) {

  def start(): Unit = {
    log("listening for external messages")

    dom.window.addEventListener(
      "message", // NOTE: This message type is required to get the message to the extension
      eventHandler,
      useCapture = true
    )
  }

  // NOTE: Anyone could be sending this message, don't trust it
  private val eventHandler = (event: dom.raw.MessageEvent) => {
    // TODO: validate the event.source
    TaggedModel.decode[Command](event.data.toString) match {
      case Failure(exception) =>
      // TODO: Remove this log, it will flood the console as it tries to parse any msg
      // log(s"Failed to decode message: $exception")

      case Success(value) =>
        log(s"Command received: $value")
        commandProcessor
          .process(value.model)
          .onComplete(reply(value.tag, _))
    }
  }

  private def reply[T: Encoder](tag: UUID, result: Try[T]): Unit = {
    val model = result match {
      case Failure(exception) => Event.CommandRejected(exception.getMessage).asJson
      case Success(value) => value.asJson
    }
    val msg = TaggedModel(tag, model)

    // NOTE: Any other tab/extension could listen to this message
    // TODO: Verify how we can use the targetOrigin properly to specify the web site as the target.
    dom.window.postMessage(msg.asJson.noSpaces, "*")
  }

  private def log(msg: String): Unit = {
    println(s"ExternalMessageProcessor: $msg")
  }
}
