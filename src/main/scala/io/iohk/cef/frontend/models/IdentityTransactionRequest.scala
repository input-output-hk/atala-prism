package io.iohk.cef.frontend.models

import io.iohk.cef.ledger.identity.IdentityTransaction

case class IdentityTransactionRequest(transaction: IdentityTransaction, ledgerId: Int)
