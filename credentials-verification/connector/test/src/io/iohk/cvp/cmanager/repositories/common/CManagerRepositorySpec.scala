package io.iohk.cvp.cmanager.repositories.common

import io.iohk.cvp.repositories.PostgresRepositorySpec

import scala.concurrent.duration._

trait CManagerRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

}
