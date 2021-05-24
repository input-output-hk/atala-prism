package io.iohk.atala.mirror.db

import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *db.CardanoWalletAddressDaoSpec"
class CardanoWalletAddressDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._
  import CardanoWalletFixtures._

  "CardanoWalletAddressDao" should {
    "insert single Cardano wallet address into the db" in {

      // when
      val resultCount = (for {
        _ <- ConnectionDao.insert(connection1)
        _ <- CardanoWalletDao.insert(cardanoWallet)
        resultCount <- CardanoWalletAddressDao.insert(cardanoWalletAddress)
      } yield resultCount)
        .transact(database)
        .runSyncUnsafe()

      // then
      resultCount mustBe 1
    }
  }

}
