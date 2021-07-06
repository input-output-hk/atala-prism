package io.iohk.atala.cvp.webextension.activetab

/**
  * The config for the current tab app.
  *
  * NOTE: Right now there is a single config for both context, the web site and the isolated context.
  *
  * @param contextScripts the scripts required to be injected into the current tab context,
  *                       so that our app actually runs there.
  */
case class ActiveTabConfig(contextScripts: Seq[String])
