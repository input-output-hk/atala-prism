package io.iohk.cef.query.ledger

import io.iohk.cef.crypto._

package object identity {
  type IdentityPartition = Set[SigningPublicKey]
  type IdentityQueryEngine = LedgerQueryEngine[IdentityPartition]
  type IdentityQueryService = LedgerQueryService[IdentityPartition, IdentityQuery]
}
