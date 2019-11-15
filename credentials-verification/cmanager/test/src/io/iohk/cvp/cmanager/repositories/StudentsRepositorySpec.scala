package io.iohk.cvp.cmanager.repositories

import java.time.LocalDate

import io.iohk.cvp.cmanager.models.requests.CreateStudent
import io.iohk.cvp.cmanager.repositories.common.CManagerRepositorySpec
import io.iohk.cvp.cmanager.repositories.common.DataPreparation._
import org.scalatest.EitherValues._

class StudentsRepositorySpec extends CManagerRepositorySpec {
  lazy val repository = new StudentsRepository(database)

  "create" should {
    "create a new student" in {
      val issuer = createIssuer("Issuer-1")
      val request = CreateStudent(issuer, "uid", "Dusty Here", "d.here@iohk.io", LocalDate.now())
      val result = repository.create(request).value.futureValue
      val student = result.right.value
      student.issuer must be(issuer)
      student.fullName must be(request.fullName)
      student.universityAssignedId must be(request.universityAssignedId)
      student.email must be(request.email)
      student.admissionDate must be(request.admissionDate)
    }
  }
}
