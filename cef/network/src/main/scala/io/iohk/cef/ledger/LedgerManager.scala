package io.iohk.cef.ledger

import scala.concurrent.Future

trait LedgerManager {

  type Transaction

  val LedgerId: Int

  def apply(transaction: Transaction): Future[Unit]
}
