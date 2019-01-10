package io.iohk.cef.frontend.models

import io.iohk.cef.LedgerId
import io.iohk.cef.crypto._
import io.iohk.cef.ledger.identity.IdentityTransactionData

case class CreateIdentityTransactionRequest(
    `type`: IdentityTransactionType,
    data: IdentityTransactionData,
    ledgerId: LedgerId,
    privateKey: SigningPrivateKey,
    linkingIdentityPrivateKey: Option[SigningPrivateKey] = None
)

case class SubmitIdentityTransactionRequest(
    `type`: IdentityTransactionType,
    data: IdentityTransactionData,
    ledgerId: LedgerId,
    signature: Signature,
    linkingIdentitySignature: Option[Signature] = None,
    claimSignature: Option[Signature] = None,
    endorseSignature: Option[Signature] = None,
    signatureFromCertificate: Option[Signature] = None
)
