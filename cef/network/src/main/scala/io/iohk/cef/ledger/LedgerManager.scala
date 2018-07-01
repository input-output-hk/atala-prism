package io.iohk.cef.ledger

import scala.concurrent.Future

trait LedgerManager {

  type Transaction

  val LedgerId: Int

  val blocks: Stream[Block[Transaction]]

  def apply(transaction: Transaction): Future[Unit]
}
