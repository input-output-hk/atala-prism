package io.iohk.atala.cvp.webextension.activetab.isolated

import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI

import scala.concurrent.{ExecutionContext, Future}

private[isolated] class CommandProcessor(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def process(cmd: Command): Future[Event] = {
    cmd match {
      case Command.GetSdkDetails =>
        val response = Event.GotSdkDetails(extensionId = chrome.runtime.Runtime.id)
        Future.successful(response)

      case Command.GetWalletStatus =>
        backgroundAPI
          .getWalletStatus()
          .map(response => Event.GotWalletStatus(response.status.toString))

      case Command.CreateSession =>
        backgroundAPI
          .login()
          .map(response => Event.GotUserSession(response))

      case Command.EnqueueRequestApproval(sessionId, request) =>
        backgroundAPI
          .enqueueRequestApproval(sessionId, request)
          .map(_ => Event.EnqueueRequestApprovalAck)

      case Command.SignConnectorRequest(sessionId, request) =>
        backgroundAPI
          .signConnectorRequest(sessionId, request)
          .map(response => Event.GotSignedResponse(response.signedMessage))

      case Command.VerifySignedCredential(sessionId, signedCredentialStringRepresentation, encodedMerkleProof) =>
        backgroundAPI
          .verifySignedCredential(sessionId, signedCredentialStringRepresentation, encodedMerkleProof)
          .map(response => Event.SignedCredentialVerified(response.result))

    }
  }.recover {
    case e => Event.CommandRejected(e.getMessage) // Any exceptions will be resolved to CommandRejected
  }

}
