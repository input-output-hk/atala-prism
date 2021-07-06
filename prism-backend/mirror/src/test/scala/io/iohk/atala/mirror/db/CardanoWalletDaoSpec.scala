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
        result <- CardanoWalletDao.insert(cardanoWallet1)
      } yield result)
        .transact(database)
        .runSyncUnsafe()

      // then
      result mustBe cardanoWallet1.id
    }

    "update last used number" in {
      // when
      val lastUsedNo = 15
      val updatedWallet = (for {
        _ <- ConnectionDao.insert(connection2)
        _ <- CardanoWalletDao.insert(cardanoWallet1)
        _ <- CardanoWalletDao.updateLastUsedNo(cardanoWallet1.id, lastUsedNo)
        updatedWalletOption <- CardanoWalletDao.findById(cardanoWallet1.id)
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
        _ <- CardanoWalletDao.insert(cardanoWallet1)
        _ <- CardanoWalletDao.updateLastGeneratedNo(cardanoWallet1.id, lastGeneratedNo)
        updatedWalletOption <- CardanoWalletDao.findById(cardanoWallet1.id)
      } yield updatedWalletOption.value)
        .transact(database)
        .runSyncUnsafe()

      // then
      updatedWallet.lastGeneratedNo mustBe lastGeneratedNo
    }

    "find by connection token" in {
      // when
      val wallets = (for {
        _ <- ConnectionDao.insert(connection2)
        _ <- CardanoWalletDao.insert(cardanoWallet1)
        _ <- CardanoWalletDao.insert(cardanoWallet2)
        wallets <- CardanoWalletDao.findByConnectionToken(connection2.token)
      } yield wallets)
        .transact(database)
        .runSyncUnsafe()

      // then
      wallets mustBe List(cardanoWallet1, cardanoWallet2)
    }
  }

}
