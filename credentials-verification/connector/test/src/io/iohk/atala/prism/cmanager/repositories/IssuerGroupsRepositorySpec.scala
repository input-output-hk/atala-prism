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
      val issuerId1 = createIssuer("Issuer-1", "a")
      val issuerId2 = createIssuer("Issuer 2", "b")

      // allows creating the same group on different issuers
      repository.create(issuerId1, groupName).value.futureValue
      repository.create(issuerId2, groupName).value.futureValue
      succeed
    }

    "fail when the group name is repeated" in {
      val issuerId = createIssuer("Issuer-1")
      val groupName = IssuerGroup.Name("IOHK 2019")

      repository.create(issuerId, groupName).value.futureValue
      intercept[Exception] {
        repository.create(issuerId, groupName).value.futureValue
      }

      succeed
    }
  }

  "getBy" should {
    "get the available groups for an issuer" in {
      val groups = List("Group 1", "Group 2").map(IssuerGroup.Name.apply)
      val issuerId1 = createIssuer("Issuer-1", "a")
      createIssuerGroup(issuerId1, groups(0))
      createIssuerGroup(issuerId1, groups(1))

      val issuerId2 = createIssuer("Issuer 2", "b")
      createIssuerGroup(issuerId2, IssuerGroup.Name("Other"))

      val result = repository.getBy(issuerId1).value.futureValue
      result.right.value must be(groups)
    }
  }
}
