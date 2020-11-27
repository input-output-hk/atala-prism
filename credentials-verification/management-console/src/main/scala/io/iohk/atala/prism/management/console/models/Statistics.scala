package io.iohk.atala.prism.management.console.models

final case class Statistics(
    numberOfContacts: Int,
    numberOfGroups: Int,
    numberOfCredentials: Int,
    numberOfCredentialsPublished: Int,
    numberOfCredentialsReceived: Int
) {
  val numberOfCredentialsInDraft: Int = numberOfCredentials - numberOfCredentialsPublished
}
