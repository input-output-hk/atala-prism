package atala.obft.common

import atala.clock._

case class TransactionSnapshot[Tx](transaction: Tx, timestamp: TimeSlot)
