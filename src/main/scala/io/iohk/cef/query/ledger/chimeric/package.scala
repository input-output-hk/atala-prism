package io.iohk.cef.query

import io.iohk.cef.ledger.chimeric.ChimericStateResult
import io.iohk.cef.query.ledger.{LedgerQueryEngine, LedgerQueryService}

package object chimeric {
  type ChimericPartition = ChimericStateResult
  type ChimericQueryEngine = LedgerQueryEngine[ChimericPartition]
  type ChimericQueryService = LedgerQueryService[ChimericPartition, ChimericQuery]
}
