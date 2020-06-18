package io.iohk.atala.cvp.webextension.activetab.isolated
import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event, UserDetails}
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
          .login(origin)
          .map(response =>
            Event.GotUserSession(UserDetails(response.sessionId, response.name, response.role, response.logo))
          )
    }
  }
}
