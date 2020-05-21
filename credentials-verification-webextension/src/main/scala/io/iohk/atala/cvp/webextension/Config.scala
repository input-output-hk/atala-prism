package io.iohk.atala.cvp.webextension

import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig

/**
  * This is the global config, which includes any configurable details.
  *
  * For convenience, there are two configs, the Default one and the one for Development.
  */
case class Config(
    activeTabConfig: activetab.ActiveTabConfig,
    connectorUrl: String
)

// TODO: REPLACE ME
object Config {

  val Default: Config = {
    Config(
      ActiveTabConfig(),
      connectorUrl = "http://cvp-develop.cef.iohkdev.io"
    )
  }

  val Dev: Config = {
    Config(
      ActiveTabConfig(),
      connectorUrl = "http://localhost:10000"
    )
  }
}
