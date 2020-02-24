package io.iohk.cvp.intdemo

import io.iohk.connector.model.TokenString
import io.iohk.connector.repositories.ConnectorRepositorySpecBase
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.util.Random
import doobie.implicits._

class CredentialStatusRepositorySpec extends ConnectorRepositorySpecBase {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

  lazy val credentialStatusRepository = new CredentialStatusRepository(database)

  "merge" should {
    "insert status when no status exists" in {
      val token = TokenString.random().token
      val status = Random.nextInt()

      val rowCount: Int = credentialStatusRepository.merge(token, status).futureValue

      rowCount shouldBe 1
      sql"""SELECT status FROM intdemo_credential_status WHERE token=$token""".runUnique[Int]() shouldBe status
    }

    "update status when one exists" in {
      val token = TokenString.random().token
      val status1 = Random.nextInt()
      val status2 = Random.nextInt()

      credentialStatusRepository.merge(token, status1).futureValue
      val rowCount = credentialStatusRepository.merge(token, status2).futureValue

      rowCount shouldBe 1
      sql"""SELECT status FROM intdemo_credential_status WHERE token=$token""".runUnique[Int]() shouldBe status2
    }
  }

  "find" should {
    "return None when no matching entries exist" in {
      val token = TokenString.random().token

      val result = credentialStatusRepository.find(token).futureValue

      result shouldBe None
    }

    "return entries created with merge" in {
      val token = TokenString.random().token
      val status = Random.nextInt()
      credentialStatusRepository.merge(token, status).futureValue

      val result: Option[Int] = credentialStatusRepository.find(token).futureValue

      result shouldBe Some(status)
    }
  }
}
