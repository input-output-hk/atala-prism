package io.iohk.atala.mirror.db

import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import scala.concurrent.duration._
import doobie.implicits._

class ExampleDaoSpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "ExampleDao" should {
    "run example query" in {
      ExampleDao.test().transact(database).unsafeRunSync() mustBe true
    }
  }
}
