package io.iohk.atala.prism.intdemo

import java.time.LocalDate
import java.util.concurrent.ThreadLocalRandom

import doobie.implicits._
import io.iohk.atala.prism.connector.model.TokenString
import io.iohk.atala.prism.connector.repositories.ConnectorRepositorySpecBase
import IntDemoRepositorySpec._
import io.iohk.atala.prism.repositories.ops.SqlTestOps.Implicits
import io.iohk.prism.intdemo.protos.intdemo_models
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.util.Random

class IntDemoRepositorySpec extends ConnectorRepositorySpecBase {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val repository = new IntDemoRepository(database)

  "mergeStatus" should {
    "insert status when no status exists" in {
      val token = TokenString.random()
      val status = intdemo_models.SubjectStatus.UNCONNECTED

      val rowCount: Int = repository.mergeSubjectStatus(token, status).futureValue

      rowCount shouldBe 1
      sql"""SELECT status FROM intdemo_credential_status WHERE token=$token"""
        .runUnique[Int]() shouldBe intdemo_models.SubjectStatus.UNCONNECTED.value
    }

    "update status when one exists" in {
      val token = TokenString.random()

      repository.mergeSubjectStatus(token, intdemo_models.SubjectStatus.UNCONNECTED).futureValue
      val rowCount = repository.mergeSubjectStatus(token, intdemo_models.SubjectStatus.CONNECTED).futureValue

      rowCount shouldBe 1
      sql"""SELECT status FROM intdemo_credential_status WHERE token=$token"""
        .runUnique[Int]() shouldBe intdemo_models.SubjectStatus.CONNECTED.value
    }
  }

  "findStatus" should {
    "return None when no matching entries exist" in {
      val token = TokenString.random()

      val result = repository.findSubjectStatus(token).futureValue

      result shouldBe None
    }

    "return entries created with merge" in {
      val token = TokenString.random()
      val status = intdemo_models.SubjectStatus.UNCONNECTED
      repository.mergeSubjectStatus(token, status).futureValue

      val result = repository.findSubjectStatus(token).futureValue

      result shouldBe Some(intdemo_models.SubjectStatus.UNCONNECTED)
    }
  }

  "mergePersonalInfo" should {
    "insert when no row exists" in {
      val token = TokenString.random()
      val v = (token, Random.nextString(12), aRandomDateOfBirth())

      val rowCount = repository.mergePersonalInfo(v._1, v._2, v._3).futureValue

      rowCount shouldBe 1
      sql"""SELECT first_name, date_of_birth FROM intdemo_id_personal_info WHERE token=$token"""
        .runUnique[(String, LocalDate)]() shouldBe ((v._2, v._3))
    }

    "update when one does" in {
      val token = TokenString.random()
      val v1 = (token, Random.nextString(12), aRandomDateOfBirth())
      val v2 = (token, Random.nextString(12), aRandomDateOfBirth())

      repository.mergePersonalInfo(v1._1, v1._2, v1._3).futureValue
      val rowCount = repository.mergePersonalInfo(v2._1, v2._2, v2._3).futureValue

      rowCount shouldBe 1
      sql"""SELECT first_name, date_of_birth FROM intdemo_id_personal_info WHERE token=$token"""
        .runUnique[(String, LocalDate)]() shouldBe ((v2._2, v2._3))
    }
  }

  "findPersonalInfo" should {
    "find entries when they exist" in {
      val token = TokenString.random()
      val expectedName = "first name"
      val expectedDoB = LocalDate.now()
      repository.mergePersonalInfo(token, expectedName, expectedDoB).futureValue

      val (actualName, actualDoB) = repository.findPersonalInfo(token).futureValue.get

      (actualName, actualDoB) shouldBe ((expectedName, expectedDoB))
    }
  }
}

object IntDemoRepositorySpec {

  def aRandomDateOfBirth(): LocalDate = {
    val minDay: Long = LocalDate.of(1970, 1, 1).toEpochDay
    val maxDay: Long = LocalDate.of(2019, 12, 31).toEpochDay
    val random: ThreadLocalRandom = ThreadLocalRandom.current()
    LocalDate.ofEpochDay(random.nextLong(minDay, maxDay))
  }
}
