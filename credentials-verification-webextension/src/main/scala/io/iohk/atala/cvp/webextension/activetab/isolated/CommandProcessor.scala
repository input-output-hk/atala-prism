package io.iohk.atala.cvp.webextension.activetab.isolated
import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event, JsUserDetails}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI
import scala.concurrent.{ExecutionContext, Future}

private[isolated] class CommandProcessor(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def process(cmd: Command)(implicit origin: String): Future[Event] = {
    cmd match {
      case Command.GetWalletStatus =>
        backgroundAPI
          .getWalletStatus()
          .map(response => Event.GotWalletStatus(response.status.toString))

      case Command.CreateSession =>
        backgroundAPI
          .login()
          .map(response => Event.GotUserSession(response))

      case Command.RequestSignature(sessionId, subject) =>
        backgroundAPI
          .requestSignature(sessionId, subject)
          .map(_ => Event.RequestSignatureAck)

      case Command.SignConnectorRequest(sessionId, request) =>
        backgroundAPI
          .signConnectorRequest(sessionId, request)
          .map(response => Event.GotSignedResponse(response.signedMessage))
    }
  }
}
