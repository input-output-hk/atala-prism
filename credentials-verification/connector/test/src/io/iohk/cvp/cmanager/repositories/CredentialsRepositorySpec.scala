package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.models.{Credential, Issuer}
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class CredentialsRepositorySpec extends CManagerRepositorySpec {

  lazy val repository = new CredentialsRepository(database)

  "create" should {
    "create a new credential" in {
      val issuer = createIssuer("Issuer-1")
      val request = CreateCredential(
        issuedBy = issuer,
        subject = "Atala Prism",
        title = "Major IN Applied Blockchain",
        enrollmentDate = LocalDate.now(),
        graduationDate = LocalDate.now().plusYears(5),
        groupName = "Computer Science"
      )

      val result = repository.create(request).value.futureValue
      val credential = result.right.value
      credential.enrollmentDate must be(request.enrollmentDate)
      credential.graduationDate must be(request.graduationDate)
      credential.issuedBy must be(request.issuedBy)
      credential.subject must be(request.subject)
      credential.title must be(request.title)
      credential.groupName must be(request.groupName)
    }
  }

  "getBy" should {
    "return the first credentials" in {
      val issuer = createIssuer("Issuer X")
      val credA = createCredential(issuer, "A")
      val credB = createCredential(issuer, "B")
      val credC = createCredential(issuer, "C")

      val result = repository.getBy(issuer, 2, None).value.futureValue.right.value
      result.toSet must be(Set(credA, credB))
    }

    "paginate by the last seen credential" in {
      val issuer = createIssuer("Issuer X")
      val credA = createCredential(issuer, "A")
      val credB = createCredential(issuer, "B")
      val credC = createCredential(issuer, "C")
      val credD = createCredential(issuer, "D")

      val first = repository.getBy(issuer, 2, None).value.futureValue.right.value
      val result = repository.getBy(issuer, 1, first.lastOption.map(_.id)).value.futureValue.right.value
      result.toSet must be(Set(credC))
    }
  }

  def createCredential(issuedBy: Issuer.Id, tag: String = ""): Credential = {
    val request = CreateCredential(
      issuedBy = issuedBy,
      subject = s"Atala Prism $tag".trim,
      title = s"Major IN Applied Blockchain $tag".trim,
      enrollmentDate = LocalDate.now(),
      graduationDate = LocalDate.now().plusYears(5),
      groupName = s"Computer Science $tag".trim
    )

    val result = repository.create(request).value.futureValue
    result.right.value
  }
}
