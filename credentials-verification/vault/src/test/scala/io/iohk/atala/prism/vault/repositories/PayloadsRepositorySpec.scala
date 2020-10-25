package io.iohk.atala.prism.vault.repositories

import java.util.UUID

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.vault.model.Payload
import org.scalatest.EitherValues

class PayloadsRepositorySpec extends PostgresRepositorySpec with EitherValues {
  lazy val repository = new PayloadsRepository(database)

  "create" should {
    "create a new payload" in {
      val id = Payload.Id(UUID.randomUUID())
      repository.create(id).value.futureValue.right.value must be(Payload(id))
    }
  }
}
