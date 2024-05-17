package io.iohk.atala.prism.node.models

import derevo.derive

import java.time.Instant
import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.node.models.{Ledger, TransactionId}
import tofu.logging.derivation.loggable

import scala.collection.immutable.IndexedSeq

case class AtalaObjectTransactionSubmission(
    atalaObjectId: AtalaObjectId,
    ledger: Ledger,
    transactionId: TransactionId,
    submissionTimestamp: Instant,
    status: AtalaObjectTransactionSubmissionStatus
)

@derive(loggable)
sealed trait AtalaObjectTransactionSubmissionStatus extends EnumEntry with UpperSnakecase

object AtalaObjectTransactionSubmissionStatus extends Enum[AtalaObjectTransactionSubmissionStatus] {
  val values: IndexedSeq[AtalaObjectTransactionSubmissionStatus] = findValues

  case object Pending extends AtalaObjectTransactionSubmissionStatus
  case object Deleted extends AtalaObjectTransactionSubmissionStatus
  case object InLedger extends AtalaObjectTransactionSubmissionStatus
}
