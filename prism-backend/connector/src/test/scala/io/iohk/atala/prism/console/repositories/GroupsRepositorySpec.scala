package io.iohk.atala.prism.console.repositories

import cats.scalatest.EitherMatchers._
import io.iohk.atala.prism.console.DataPreparation._
import io.iohk.atala.prism.console.models.IssuerGroup
import io.iohk.atala.prism.AtalaWithPostgresSpec
import org.scalatest.OptionValues._

import scala.concurrent.duration._

class GroupsRepositorySpec extends AtalaWithPostgresSpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val repository = new GroupsRepository(database)

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

      val result = repository.getBy(issuerId1, None).value.futureValue.toOption.value
      result.map(_.value.name) must be(groups)
    }

    "includes the contact count" in {
      val groups = List("Group 1", "Group 2").map(IssuerGroup.Name.apply)
      val issuerId = createIssuer("Issuer-1", "a")
      createIssuerGroup(issuerId, groups(0))
      createIssuerGroup(issuerId, groups(1))
      createContact(issuerId, "test-contact-1", groups(0))
      createContact(issuerId, "test-contact-2", groups(0))
      createContact(issuerId, "test-contact-3", groups(1))

      val result = repository.getBy(issuerId, None).value.futureValue
      result.map(_.map(_.numberOfContacts)) must beRight(List(2, 1))
    }

    "allows filtering by contact" in {
      val groups = List("Group 1", "Group 2").map(IssuerGroup.Name.apply)
      val issuerId = createIssuer("Issuer-1", "a")
      groups.foreach { g => createIssuerGroup(issuerId, g) }
      createContact(issuerId, "test-contact-1", groups(0))
      val contact = createContact(issuerId, "test-contact-2", groups(0))
      createContact(issuerId, "test-contact-3", groups(1))

      val result = repository.getBy(issuerId, Some(contact.contactId)).value.futureValue.toOption.value
      result.size must be(1)

      val resultGroup = result.head
      resultGroup.value.name must be(groups(0))
      resultGroup.numberOfContacts must be(2)
    }
  }
}
