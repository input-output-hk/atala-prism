package io.iohk.cvp.cmanager.repositories

import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class IssuerGroupsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new IssuerGroupsRepository(database)

  "create" should {
    "allow creating different groups" in {
      val issuer1 = createIssuer("Issuer-1", "a")
      val issuer2 = createIssuer("Issuer 2", "b")

      // allows creating the same group on different issuers
      repository.create(issuer1.id, "IOHK 2019").value.futureValue
      repository.create(issuer2.id, "IOHK 2019").value.futureValue
      succeed
    }

    "fail when the group name is repeated" in {
      val issuer = createIssuer("Issuer-1")

      repository.create(issuer.id, "IOHK 2019").value.futureValue
      intercept[Exception] {
        repository.create(issuer.id, "IOHK 2019").value.futureValue
      }

      succeed
    }
  }

  "getBy" should {
    "get the available groups for an issuer" in {
      val issuer1 = createIssuer("Issuer-1", "a")
      createIssuerGroup(issuer1.id, "Group 1")
      createIssuerGroup(issuer1.id, "Group 2")

      val issuer2 = createIssuer("Issuer 2", "b")
      createIssuerGroup(issuer2.id, "Other")

      val result = repository.getBy(issuer1.id).value.futureValue
      result.right.value must be(List("Group 1", "Group 2"))
    }
  }
}
