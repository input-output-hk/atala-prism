package io.iohk.cef.ledger

import io.iohk.cef.db.AutoRollbackSpec
import org.scalatest.{MustMatchers, fixture}

class LedgerSpec extends fixture.FlatSpec with AutoRollbackSpec with MustMatchers {
  behavior of "Ledger"

  it should "apply a block" in { session =>
    pending
  }
}
