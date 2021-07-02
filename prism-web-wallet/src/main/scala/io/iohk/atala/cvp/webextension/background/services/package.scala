package io.iohk.atala.cvp.webextension.background

import scalapb.GeneratedMessage
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.crypto.keys.ECKeyPair
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.extras.RequestUtils
import typings.inputOutputHkPrismSdk.mod.io.iohk.atala.prism.kotlin.identity.{DID, DIDCompanion}

import java.util.Base64
import scala.scalajs.js.typedarray.byteArray2Int8Array

package object services {
  def metadataForRequest[Request <: GeneratedMessage](
      ecKeyPair: ECKeyPair,
      did: DID,
      request: Request
  ): Map[String, String] = {
    val metadata = RequestUtils.generateBytesMetadata(
      did.value,
      ecKeyPair.privateKey,
      byteArray2Int8Array(request.toByteArray),
      null
    )
    Map(
      "did" -> did.value,
      "didKeyId" -> DIDCompanion.masterKeyId,
      "didSignature" -> Base64.getUrlEncoder.encodeToString(metadata.didSignature.toArray),
      "requestNonce" -> Base64.getUrlEncoder.encodeToString(metadata.requestNonce.toArray)
    )
  }
}
