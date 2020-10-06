package io.iohk.atala.mirror.fixtures

import java.time.{LocalDateTime, ZoneOffset}

import io.iohk.atala.mirror.models.UserCredential
import io.iohk.atala.mirror.models.UserCredential.{IssuersDID, MessageId, MessageReceivedDate, RawCredential}

object UserCredentialFixtures {
  import ConnectionFixtures._

  val userCredential1: UserCredential =
    UserCredential(
      connection1.token,
      RawCredential("rawCredentials1"),
      Some(IssuersDID("issuersDID1")),
      MessageId("messageId1"),
      MessageReceivedDate(LocalDateTime.of(2020, 10, 4, 0, 0).toInstant(ZoneOffset.UTC))
    )

  val userCredential2: UserCredential =
    UserCredential(
      connection2.token,
      RawCredential("rawCredentials2"),
      None,
      MessageId("messageId2"),
      MessageReceivedDate(LocalDateTime.of(2020, 10, 5, 0, 0).toInstant(ZoneOffset.UTC))
    )

}
