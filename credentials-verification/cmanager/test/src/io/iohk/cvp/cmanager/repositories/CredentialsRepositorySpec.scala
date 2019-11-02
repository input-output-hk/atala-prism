package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate
import java.util.UUID

import io.iohk.cvp.cmanager.models.Issuer
import io.iohk.cvp.cmanager.models.requests.CreateCredential
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
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

  def createIssuer(name: String = "Issuer"): Issuer.Id = {
    import doobie.implicits._
    import daos._

    val id = Issuer.Id(UUID.randomUUID())
    val did = "did:geud:issuer-x"
    sql"""
         |INSERT INTO issuers (issuer_id, name, did)
         |VALUES ($id, $name, $did)
         |""".stripMargin.update.run.transact(database).unsafeRunSync()
    id
  }
}
