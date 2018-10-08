package io.iohk.cef.frontend.models

import io.iohk.cef.crypto._
import spray.json._
import io.iohk.cef.LedgerId

case class CreateIdentityTransactionRequest(
    `type`: IdentityTransactionType,
    identity: String,
    ledgerId: LedgerId,
    publicKey: SigningPublicKey,
    privateKey: SigningPrivateKey)

object CreateIdentityTransactionRequest {
  implicit val sprayJsonFormat: RootJsonFormat[CreateIdentityTransactionRequest] =
    jsonFormat5(CreateIdentityTransactionRequest.apply)
}

case class SubmitIdentityTransactionRequest(
    `type`: IdentityTransactionType,
    identity: String,
    ledgerId: LedgerId,
    publicKey: SigningPublicKey,
    signature: Signature)
