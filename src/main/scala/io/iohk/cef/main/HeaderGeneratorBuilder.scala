package io.iohk.cef.main
import io.iohk.cef.ledger.{BlockHeader, Transaction}

trait HeaderGeneratorBuilder[S, H <: BlockHeader] {
  val headerGenerator: Seq[Transaction[S]] => H
}
