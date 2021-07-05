package io.iohk.atala.prism.kycbridge.models.identityMind

final case class ConsumerResponse(
    user: String, // The current reputation of the user involved in this transaction,
    upr: Option[
      String
    ], // The previous reputation of the User, that is, the reputation of the user the last time that it was evaluated
    frn: Option[String], // The name of the fraud rule that fired
    frp: Option[String], // Result of fraud evaluation
    frd: Option[String], // The description of the fraud rule that fired
    arpr: Option[String] // Result of automated review evaluation
)
