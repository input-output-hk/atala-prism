package io.iohk.atala.prism.vault.repositories

import java.util.UUID

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.vault.model.Payload
import cats.scalatest.EitherMatchers._

class PayloadsRepositorySpec extends PostgresRepositorySpec {
  lazy val repository = new PayloadsRepository(database)

  "create" should {
    "create a new payload" in {
      val id = Payload.Id(UUID.randomUUID())
      repository.create(id).value.futureValue must beRight(Payload(id))
    }
  }
}
