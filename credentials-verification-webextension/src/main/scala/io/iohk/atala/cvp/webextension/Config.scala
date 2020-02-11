package io.iohk.atala.cvp.webextension

import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig
import io.iohk.atala.cvp.webextension.background.services.http.HttpService

/**
  * This is the global config, which includes any configurable details.
  *
  * For convenience, there are two configs, the Default one and the one for Development.
  */
case class Config(
    httpConfig: HttpService.Config,
    activeTabConfig: activetab.ActiveTabConfig
)

// TODO: REPLACE ME
object Config {

  val Default: Config = {
    Config(
      HttpService.Config(serverUrl = "https://safer.chat/api"),
      ActiveTabConfig()
    )
  }

  val Dev: Config = {
    Config(
      HttpService.Config(serverUrl = "http://localhost:9000"),
      ActiveTabConfig()
    )
  }
}
