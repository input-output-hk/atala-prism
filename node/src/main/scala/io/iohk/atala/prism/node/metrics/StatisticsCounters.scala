package io.iohk.atala.prism.node.metrics

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._

object StatisticsCounters {

  sealed trait MetricCounter extends EnumEntry with UpperSnakecase

  object MetricCounter extends Enum[MetricCounter] {
    val values = findValues

    case object NumberOfPendingOperations extends MetricCounter
    case object NumberOfPublishedDids extends MetricCounter
    case object NumberOfIssuedCredentialBatches extends MetricCounter
    case object NumberOfCredentialsRevoked extends MetricCounter
    case object NumberOfAppliedTransactions extends MetricCounter
    case object NumberOfRejectedTransactions extends MetricCounter
  }
}
