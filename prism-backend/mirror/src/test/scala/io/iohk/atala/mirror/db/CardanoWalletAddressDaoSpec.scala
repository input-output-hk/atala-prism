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
        _ <- ConnectionDao.insert(connection2)
        _ <- CardanoWalletDao.insert(cardanoWallet)
        resultCount <- CardanoWalletAddressDao.insert(cardanoWalletAddress1)
      } yield resultCount)
        .transact(database)
        .runSyncUnsafe()

      // then
      resultCount mustBe 1
    }

    "find wallet addresses by connection token with wallet name" in {
      // when
      val walletAddresses = (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoWalletFixtures.insertAll(database)
        walletAddresses <-
          CardanoWalletAddressDao.findByConnectionTokenWithWalletName(connection2.token).transact(database)
      } yield walletAddresses)
        .runSyncUnsafe()

      // then
      walletAddresses.size mustBe 2

      walletAddresses.head.cardanoWalletAddress mustBe cardanoWalletAddress1
      walletAddresses.head.walletName mustBe cardanoWallet.name

      walletAddresses(1).cardanoWalletAddress mustBe cardanoWalletAddress2
      walletAddresses(1).walletName mustBe cardanoWallet.name
    }
  }

}
