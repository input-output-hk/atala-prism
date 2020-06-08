package io.iohk.atala.cvp.webextension.activetab.isolated
import io.iohk.atala.cvp.webextension.activetab.models.{Command, Event, UserDetails}
import io.iohk.atala.cvp.webextension.background.BackgroundAPI

import scala.concurrent.{ExecutionContext, Future}

private[isolated] class CommandProcessor(backgroundAPI: BackgroundAPI)(implicit ec: ExecutionContext) {

  def process(cmd: Command): Future[Event] = {
    cmd match {
      case Command.GetWalletStatus() =>
        backgroundAPI
          .getWalletStatus()
          .map(response => Event.GotWalletStatus(response.status.toString))

      case Command.GetUserDetails() =>
        backgroundAPI
          .getUserDetails()
          .map(response => Event.GotUserDetails(response.map(u => UserDetails(u.name, u.role, u.logo))))
    }
  }
}
