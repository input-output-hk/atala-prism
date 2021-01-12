package io.iohk.atala.cvp.webextension.activetab.models

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll
@JSExportAll
final case class JsUserDetails(sessionId: String, name: String, role: String, logo: js.Array[Byte])
