package io.iohk.atala.prism.management.console.models

import java.time.Instant

final case class TimeInterval(
    start: Instant,
    end: Instant
)

final case class GetStatistics(
    timeInterval: Option[TimeInterval]
)

final case class Statistics(
    numberOfContacts: Int,
    numberOfGroups: Int,
    numberOfCredentials: Int,
    numberOfCredentialsPublished: Int,
    numberOfCredentialsReceived: Int
) {
  val numberOfCredentialsInDraft: Int =
    numberOfCredentials - numberOfCredentialsPublished
}
