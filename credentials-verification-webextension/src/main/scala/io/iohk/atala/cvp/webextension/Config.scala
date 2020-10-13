package io.iohk.atala.cvp.webextension

import io.iohk.atala.cvp.webextension.activetab.ActiveTabConfig

/**
  * This is the global config, which includes any configurable details.
  *
  * For convenience, there are two configs, the Default one and the one for Development.
  */
case class Config(
    activeTabConfig: activetab.ActiveTabConfig,
    backendUrl: String,
    blockchainExplorerUrl: String,
    termsUrl: String,
    privacyPolicyUrl: String
)

object Config {

  def default(
      activeTabContextScripts: Seq[String],
      overrideConnectorUrl: Option[String] = None,
      blockchainExplorerUrl: Option[String] = None,
      termsUrl: Option[String] = None,
      privacyPolicyUrl: Option[String] = None
  ): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      backendUrl = overrideConnectorUrl.getOrElse("https://console-develop.atalaprism.io:4433"),
      blockchainExplorerUrl =
        blockchainExplorerUrl.getOrElse("https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=%s"),
      termsUrl = termsUrl.getOrElse("https://legal.atalaprism.io/terms-and-conditions.html"),
      privacyPolicyUrl = privacyPolicyUrl.getOrElse("https://legal.atalaprism.io/privacy-policy.html")
    )
  }

  def dev(
      activeTabContextScripts: Seq[String],
      overrideConnectorUrl: Option[String] = None,
      blockchainExplorerUrl: Option[String] = None,
      termsUrl: Option[String] = None,
      privacyPolicyUrl: Option[String] = None
  ): Config = {
    Config(
      ActiveTabConfig(contextScripts = activeTabContextScripts),
      backendUrl = overrideConnectorUrl.getOrElse("http://localhost:10000"),
      blockchainExplorerUrl.getOrElse("https://explorer.cardano-testnet.iohkdev.io/en/transaction?id=%s"),
      termsUrl = termsUrl.getOrElse("https://legal.atalaprism.io/terms-and-conditions.html"),
      privacyPolicyUrl = privacyPolicyUrl.getOrElse("https://legal.atalaprism.io/privacy-policy.html")
    )
  }
}
