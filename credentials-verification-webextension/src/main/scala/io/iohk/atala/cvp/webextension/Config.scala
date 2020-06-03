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

object Config {

  def default(activeTabContextScripts: Seq[String]): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      connectorUrl = "http://console-develop.atalaprism.io/"
    )
  }

  def dev(activeTabContextScripts: Seq[String]): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      connectorUrl = "http://localhost:10000"
    )
  }
}
