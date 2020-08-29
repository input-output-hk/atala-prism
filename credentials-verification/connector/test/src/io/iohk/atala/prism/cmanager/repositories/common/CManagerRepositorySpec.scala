package io.iohk.atala.prism.cmanager.repositories.common

import io.iohk.atala.prism.repositories.PostgresRepositorySpec

import scala.concurrent.duration._

trait CManagerRepositorySpec extends PostgresRepositorySpec {

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 5.millis)

}
