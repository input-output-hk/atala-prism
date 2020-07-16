package io.iohk.atala.cvp.webextension.activetab.models

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll
@JSExportAll
final case class JsSignedMessage(did: String, didKeyId: String, signature: js.Array[Byte])
