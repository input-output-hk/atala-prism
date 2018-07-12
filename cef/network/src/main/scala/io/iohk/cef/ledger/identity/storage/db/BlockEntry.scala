package io.iohk.cef.ledger.identity.storage.db

import io.iohk.cef.ledger.identity.{IdentityBlockHeader, IdentityTransaction}

case class BlockEntry(id: Long, header: IdentityBlockHeader, transaction: IdentityTransaction)
