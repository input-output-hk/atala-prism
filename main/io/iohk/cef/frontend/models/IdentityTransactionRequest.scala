package io.iohk.cef.frontend.models

import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.identity.IdentityTransactionData
import io.iohk.crypto._

case class CreateIdentityTransactionRequest(
    data: IdentityTransactionData,
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
