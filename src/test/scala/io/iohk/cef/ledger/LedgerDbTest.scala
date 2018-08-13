package io.iohk.cef.ledger

import org.scalatest.{MustMatchers, fixture}
import scalikejdbc.scalatest.AutoRollback

trait LedgerDbTest extends fixture.FlatSpec with AutoRollback with MustMatchers {
  behavior of "Ledger"

  it should "apply a block" in { session =>
    pending
  }
}
