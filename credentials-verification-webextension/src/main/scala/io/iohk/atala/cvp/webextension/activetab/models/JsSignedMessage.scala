package io.iohk.atala.cvp.webextension.activetab.models

import io.iohk.atala.prism.identity.DID

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
final case class JsSignedMessage(did: DID, didKeyId: String, encodedSignature: String, encodedNonce: String)
