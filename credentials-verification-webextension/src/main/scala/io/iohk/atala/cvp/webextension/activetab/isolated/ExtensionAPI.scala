package io.iohk.atala.cvp.webextension.activetab.isolated

import java.util.UUID

import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event, TaggedModel}
import io.iohk.atala.cvp.webextension.common.models.{ConnectorRequest, CredentialSubject}
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * While Chrome provides a way to communicate directly from web sites to the extension, it
  * doesn't fix the potential security problems, still any other extension available to the
  * web site is a potential a man-in-the-middle, and this Chrome approach limit us as we
  * would need to specify the whitelisted domains that can interact with out extension.
  *
  * While native js messages have the same problems, they don't have the previous limitation as
  * any web site will be able to communicate with our extension, the drawback is that the website
  * needs to communicate with the content-script which communicates with the background script
  * instead of the direct communication supported by the Chrome approach.
  *
  * As of now, the conclusion is that the Chrome approach drawbacks are worse than requiring a bit
  * more code and latency to support any website.
  *
  * NOTE: This API is intended to run on any other context different to the isolated content-script
  *       so that other contexts can communicate with the content-script in a simple way.
  *
  * @see https://developer.chrome.com/extensions/messaging#external-webpage
  */
class ExtensionAPI()(implicit ec: ExecutionContext) {

  def getWalletStatus(): Future[Event.GotWalletStatus] = {
    val cmd = Command.GetWalletStatus
    processCommand(cmd).collect {
      case r: Event.GotWalletStatus => r
      case x => throw new RuntimeException(s"Unknown response: $x")
    }
  }

  def login(): Future[Event.GotUserSession] = {
    val cmd = Command.CreateSession
    processCommand(cmd).collect {
      case r: Event.GotUserSession => r
      case x => throw new RuntimeException(s"Unknown response: $x")
    }
  }

  def requestSignature(sessionId: String, subject: CredentialSubject): Future[Unit] = {
    val cmd = Command.RequestSignature(sessionId, subject)
    processCommand(cmd).collect {
      case Event.RequestSignatureAck => ()
      case x => throw new RuntimeException(s"Unknown response: $x")
    }
  }

  def signConnectorRequest(sessionId: String, request: ConnectorRequest): Future[Event.GotSignedResponse] = {
    val cmd = Command.SignConnectorRequest(sessionId, request)
    processCommand(cmd).collect {
      case r: Event.GotSignedResponse => r
      case x => throw new RuntimeException(s"Unknown response: $x")
    }
  }

  def verifySignedCredential(
      sessionId: String,
      signedCredentialStringRepresentation: String
  ): Future[Event.SignedCredentialVerified] = {
    val cmd = Command.VerifySignedCredential(sessionId, signedCredentialStringRepresentation)
    processCommand(cmd).collect {
      case r: Event.SignedCredentialVerified => r
      case x => throw new RuntimeException(s"Unknown response: $x")
    }
  }

  /**
    * Process a command, waiting for a response.
    *
    * @param cmd the command to process.
    * @return The actual response.
    */
  private def processCommand(cmd: Command): Future[Event] = {
    val tagged = TaggedModel(UUID.randomUUID(), cmd)
    val msg = tagged.asJson.noSpaces
    // subscribe for the result before sending the command to avoid latency-related race-conditions
    val result = listenFor(tagged.tag)

    // This ensures that the message gets only to the current website
    // preventing other windows to grab it, BUT it doesn allow other extensions
    // accessing the current website to grab it.
    //
    // TODO: A safer way could be to send the message to the frame where our content-script runs
    dom.window.postMessage(msg, dom.window.location.origin.orNull)

    result
  }

  /**
    * Listen to the message stream waiting for a specific message matching the given tag.
    *
    * TODO: Instead of subscribing on each request it may be more efficient to keep a single
    *       stream for all messages.
    *
    * @param tag the tag to look for.
    * @return the actual message matching the model and the tag.
    */
  private def listenFor(tag: UUID): Future[Event] = {
    val promise = Promise[Event]()
    val listener = (event: dom.raw.MessageEvent) => {
      // To reduce security risks, let's accept messages only from the website,
      // which is where our content-script runs
      dom.window.location.origin
        .filter(_ == event.origin)
        .foreach { _ =>
          TaggedModel
            .decode[Event](event.data.toString)
            .filter(_.tag == tag)
            .foreach { result =>
              // As the listener is de-registered asynchronously, it will catch more events until that's done.
              if (!promise.isCompleted) {
                promise.success(result.model)
              }
            }
        }
    }

    dom.window.addEventListener(
      "message", // NOTE: This message type is required to get the message to the extension
      listener,
      useCapture = true
    )

    // deregister the listener to avoid receiving more messages that will get ignored
    promise.future.foreach { _ =>
      dom.window.removeEventListener("message", listener, useCapture = true)
    }

    promise.future
  }

}
