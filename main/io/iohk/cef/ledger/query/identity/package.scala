package io.iohk.cef.ledger.query

import io.iohk.cef.ledger.identity.IdentityData

package object identity {
  type IdentityPartition = IdentityData
  type IdentityQueryEngine = LedgerQueryEngine[IdentityPartition]
  type IdentityQueryService = LedgerQueryService[IdentityPartition, IdentityQuery]
}
