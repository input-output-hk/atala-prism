package io.iohk.atala.prism.cmanager.repositories

import io.iohk.atala.prism.cmanager.models.IssuerGroup
import io.iohk.atala.prism.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.atala.prism.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class IssuerGroupsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new IssuerGroupsRepository(database)

  "create" should {
    "allow creating different groups" in {
      val groupName = IssuerGroup.Name("IOHK 2019")
      val issuer1 = createIssuer("Issuer-1", "a")
      val issuer2 = createIssuer("Issuer 2", "b")

      // allows creating the same group on different issuers
      repository.create(issuer1.id, groupName).value.futureValue
      repository.create(issuer2.id, groupName).value.futureValue
      succeed
    }

    "fail when the group name is repeated" in {
      val issuer = createIssuer("Issuer-1")
      val groupName = IssuerGroup.Name("IOHK 2019")

      repository.create(issuer.id, groupName).value.futureValue
      intercept[Exception] {
        repository.create(issuer.id, groupName).value.futureValue
      }

      succeed
    }
  }

  "getBy" should {
    "get the available groups for an issuer" in {
      val groups = List("Group 1", "Group 2").map(IssuerGroup.Name.apply)
      val issuer1 = createIssuer("Issuer-1", "a")
      createIssuerGroup(issuer1.id, groups(0))
      createIssuerGroup(issuer1.id, groups(1))

      val issuer2 = createIssuer("Issuer 2", "b")
      createIssuerGroup(issuer2.id, IssuerGroup.Name("Other"))

      val result = repository.getBy(issuer1.id).value.futureValue
      result.right.value must be(groups)
    }
  }
}
