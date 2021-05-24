package io.iohk.atala.mirror.db

import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *db.CardanoWalletDaoSpec"
class CardanoWalletDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._
  import CardanoWalletFixtures._

  "CardanoWalletDao" should {
    "insert single Cardano wallet into the db" in {

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection1)
        result <- CardanoWalletDao.insert(cardanoWallet)
      } yield result)
        .transact(database)
        .runSyncUnsafe()

      // then
      result mustBe cardanoWallet.id
    }
  }

}
