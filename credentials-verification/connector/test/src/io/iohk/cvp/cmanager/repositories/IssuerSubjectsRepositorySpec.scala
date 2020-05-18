package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.circe.Json
import io.iohk.cvp.cmanager.models.IssuerGroup
import io.iohk.cvp.cmanager.models.requests.CreateSubject
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class IssuerSubjectsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new IssuerSubjectsRepository(database)

  "create" should {
    "create a new subject" in {
      val issuer = createIssuer("Issuer-1").id
      val group = createIssuerGroup(issuer, IssuerGroup.Name("Grp 1"))
      val json = Json.obj(
        "universityId" -> Json.fromString("uid"),
        "name" -> Json.fromString("Dusty Here"),
        "email" -> Json.fromString("d.here@iohk.io"),
        "admissionDate" -> Json.fromString(LocalDate.now().toString)
      )
      val request = CreateSubject(issuer, group.name, json)

      val result = repository.create(request).value.futureValue
      val subject = result.right.value
      subject.groupName must be(group.name)
      subject.data must be(json)
    }
  }
}
