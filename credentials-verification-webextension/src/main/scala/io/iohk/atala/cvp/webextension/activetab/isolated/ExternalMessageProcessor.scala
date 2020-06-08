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

  private val eventHandler = (event: dom.raw.MessageEvent) => {
    // Anyone can use our extension right now, there is no need to validate the origin
    // we just need to handle the message as untrusted.
    TaggedModel.decode[Command](event.data.toString) match {
      case Failure(_) =>
        // There is nothing to do when we get an unknown message
        ()

      case Success(value) =>
        commandProcessor
          .process(value.model)
          .onComplete(reply(event.origin, value.tag, _))
    }
  }

  private def reply[T: Encoder](origin: String, tag: UUID, result: Try[T]): Unit = {
    val model = result match {
      case Failure(exception) => Event.CommandRejected(exception.getMessage).asJson
      case Success(value) => value.asJson
    }
    val msg = TaggedModel(tag, model)

    // Ensures only the origin can see the response
    dom.window.postMessage(msg.asJson.noSpaces, origin)
  }

  private def log(msg: String): Unit = {
    println(s"ExternalMessageProcessor: $msg")
  }
}
