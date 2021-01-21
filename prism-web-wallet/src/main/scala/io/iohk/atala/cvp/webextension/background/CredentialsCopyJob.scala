package io.iohk.atala.cvp.webextension.background

import io.grpc.stub.{ClientCallStreamObserver, StreamObserver}
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.console.ConsoleClientService
import io.iohk.atala.cvp.webextension.util.RetryableFuture
import io.iohk.atala.cvp.webextension.util.Scheduler.Implicits.jsScheduler
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.credential_models.AtalaMessage.Message
import io.iohk.atala.prism.protos.{connector_api, connector_models, credential_models, console_api}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CredentialsCopyJob(
    connectorClientService: ConnectorClientService,
    consoleClientService: ConsoleClientService
) {
  private var clientCallStreamObserver: Option[ClientCallStreamObserver] = None

  private def safeSetClientObserver(newSO: ClientCallStreamObserver): Unit =
    synchronized {
      // if present, we first stop the current observer
      clientCallStreamObserver.foreach(_.cancel())
      clientCallStreamObserver = Some(newSO)
    }

  private def safeCleanClientObserver(): Unit =
    synchronized {
      // if present, we cancel the current observer
      clientCallStreamObserver.foreach { stream =>
        Logger.log("Canceling the new credentials stream")
        stream.cancel()
      }
      clientCallStreamObserver = None
    }

  def start(ecKeys: ECKeyPair, did: DID)(implicit ec: ExecutionContext): Unit = {
    // everything is retried to solve the case when the DID creation tx hasn't been confirmed by Cardano
    val shouldRetry: Try[ClientCallStreamObserver] => Boolean = {
      case Success(_) => false
      case Failure(ex) =>
        Logger.log(s"Failed to connect to the new credentials stream, retrying..., error = ${ex.getMessage}")
        true
    }

    RetryableFuture
      .withExponentialBackoff[ClientCallStreamObserver](initialDelay = 1.second, maxDelay = 20.minutes)(shouldRetry) {
        startWithoutRetries(ecKeys, did)
      }
      .onComplete {
        case Failure(exception) =>
          Logger.log("Failed to initialize credentials copy stream: " + exception.getMessage)
        case Success(clientCallStreamObserver) =>
          safeSetClientObserver(clientCallStreamObserver)
          Logger.log("Successfully initialized credentials copy job")
      }
  }

  private def startWithoutRetries(ecKeys: ECKeyPair, did: DID)(implicit
      ec: ExecutionContext
  ): Future[ClientCallStreamObserver] = {
    for {
      latestCredentialExternalIdResponse <- consoleClientService.getLatestCredentialExternalId(ecKeys, did)
      lastSeenMessageId = latestCredentialExternalIdResponse.latestCredentialExternalId
      _ = Logger.log(s"Last credential external id identified $lastSeenMessageId")
    } yield connectorClientService.getMessageStream(
      ecKeys,
      did,
      CredentialsCopyJob.streamObserver(ecKeys, did, consoleClientService),
      lastSeenMessageId
    )
  }

  def stop(): Unit = safeCleanClientObserver()
}

object CredentialsCopyJob {
  def buildRequestFromConnectorMessage(
      receivedMessage: connector_models.ReceivedMessage
  ): Either[Throwable, console_api.StoreCredentialRequest] = {

    Try { credential_models.AtalaMessage.parseFrom(receivedMessage.message.toArray) } match {
      case Failure(exception) => Left(exception)
      case Success(credential_models.AtalaMessage(Message.PlainCredential(credentialMessage), _)) =>
        Right(
          console_api
            .StoreCredentialRequest()
            .withConnectionId(receivedMessage.connectionId)
            .withCredentialExternalId(receivedMessage.id)
            .withEncodedSignedCredential(credentialMessage.encodedCredential)
        )
      case Success(_) => // Other AtalaMessages
        Left(new RuntimeException("The message received was not a plain text credential"))
    }
  }

  // Note: the key pair will live in memory from now on
  private def streamObserver(
      ecKeys: ECKeyPair,
      did: DID,
      consoleClientService: ConsoleClientService
  ): StreamObserver[connector_api.GetMessageStreamResponse] =
    new StreamObserver[connector_api.GetMessageStreamResponse] {
      override def onNext(value: connector_api.GetMessageStreamResponse): Unit = {
        buildRequestFromConnectorMessage(value.getMessage) match {
          case Left(error) =>
            println(s"Failed to process credential received from connector.\nError: ${error.getMessage}")
          case Right(request) =>
            consoleClientService.storeCredentials(ecKeys, did, request)
        }
      }

      override def onError(throwable: Throwable): Unit = {
        println("Error while processing a credential message. Error:\n " + throwable)
      }

      override def onCompleted(): Unit = () // if the stream is completed, we have nothing to do
    }
}
