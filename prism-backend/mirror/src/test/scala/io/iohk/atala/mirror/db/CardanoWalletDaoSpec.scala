package io.iohk.atala.mirror.db

import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global
import org.scalatest.OptionValues._

// sbt "project mirror" "testOnly *db.CardanoWalletDaoSpec"
class CardanoWalletDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._
  import CardanoWalletFixtures._

  "CardanoWalletDao" should {
    "insert single Cardano wallet into the db" in {

      // when
      val result = (for {
        _ <- ConnectionDao.insert(connection2)
        result <- CardanoWalletDao.insert(cardanoWallet)
      } yield result)
        .transact(database)
        .runSyncUnsafe()

      // then
      result mustBe cardanoWallet.id
    }

    "update last used number" in {
      // when
      val lastUsedNo = 15
      val updatedWallet = (for {
        _ <- ConnectionDao.insert(connection2)
        _ <- CardanoWalletDao.insert(cardanoWallet)
        _ <- CardanoWalletDao.updateLastUsedNo(cardanoWallet.id, lastUsedNo)
        updatedWalletOption <- CardanoWalletDao.findById(cardanoWallet.id)
      } yield updatedWalletOption.value)
        .transact(database)
        .runSyncUnsafe()

      // then
      updatedWallet.lastUsedNo mustBe Some(lastUsedNo)
    }

    "update last generated number" in {
      // when
      val lastGeneratedNo = 15
      val updatedWallet = (for {
        _ <- ConnectionDao.insert(connection2)
        _ <- CardanoWalletDao.insert(cardanoWallet)
        _ <- CardanoWalletDao.updateLastGeneratedNo(cardanoWallet.id, lastGeneratedNo)
        updatedWalletOption <- CardanoWalletDao.findById(cardanoWallet.id)
      } yield updatedWalletOption.value)
        .transact(database)
        .runSyncUnsafe()

      // then
      updatedWallet.lastGeneratedNo mustBe lastGeneratedNo
    }
  }

}
