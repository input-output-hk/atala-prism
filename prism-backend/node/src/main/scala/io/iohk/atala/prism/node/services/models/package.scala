package io.iohk.atala.prism.node.services

import derevo.derive
import io.iohk.atala.prism.models.TransactionInfo
import io.iohk.atala.prism.protos.node_internal
import tofu.logging.derivation.loggable

import scala.concurrent.Future

package object models {
  case class AtalaObjectNotification(
      atalaObject: node_internal.AtalaObject,
      transaction: TransactionInfo
  )

  @derive(loggable)
  case class RetryOldPendingTransactionsResult(pendingTransactions: Int, numInLedgerSynced: Int, numPublished: Int)

  type AtalaObjectNotificationHandler = AtalaObjectNotification => Future[Unit]
}
