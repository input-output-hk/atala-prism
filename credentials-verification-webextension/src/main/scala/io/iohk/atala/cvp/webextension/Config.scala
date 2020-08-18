package io.iohk.atala.cvp.webextension

import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig

/**
  * This is the global config, which includes any configurable details.
  *
  * For convenience, there are two configs, the Default one and the one for Development.
  */
case class Config(
    activeTabConfig: activetab.ActiveTabConfig,
    backendUrl: String
)

object Config {

  def default(activeTabContextScripts: Seq[String], overrideConnectorUrl: Option[String] = None): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      backendUrl = overrideConnectorUrl.getOrElse("https://console-develop.atalaprism.io:4433")
    )
  }

  def dev(activeTabContextScripts: Seq[String], overrideConnectorUrl: Option[String] = None): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      backendUrl = overrideConnectorUrl.getOrElse("http://localhost:10000")
    )
  }
}
