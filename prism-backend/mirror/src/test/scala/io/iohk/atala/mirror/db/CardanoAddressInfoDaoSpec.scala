package io.iohk.atala.mirror.db

import cats.data.NonEmptyList
import monix.eval.Task
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.CardanoAddress
import io.iohk.atala.prism.models.ConnectorMessageId
import monix.execution.Scheduler.Implicits.global

// sbt "project mirror" "testOnly *db.CardanoAddressInfoDaoSpec"
class CardanoAddressInfoDaoSpec extends PostgresRepositorySpec[Task] with MirrorFixtures {
  import ConnectionFixtures._, CardanoAddressInfoFixtures._

  "CardanoAddressDao" should {
    "insert single cardano address into the db" in {
      // when
      val resultCount = (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- CardanoAddressInfoDao.insert(cardanoAddressInfo1)
      } yield resultCount)
        .transact(database)
        .runSyncUnsafe()

      // then
      resultCount mustBe 1
    }

    "return cardano addresses by address" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao
          .findBy(NonEmptyList.of(cardanoAddressInfo1.cardanoAddress, cardanoAddressInfo2.cardanoAddress))
          .transact(database)
          .runSyncUnsafe()

      // then
      cardanoAddressesInfo.toSet mustBe Set(cardanoAddressInfo1, cardanoAddressInfo2)
    }

    "return cardano addresses by connection token and cardano network" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao
          .findBy(connection1.token, cardanoAddressInfo1.cardanoNetwork)
          .transact(database)
          .runSyncUnsafe()

      // then
      cardanoAddressesInfo mustBe List(cardanoAddressInfo1)
    }

    "return none if a cardano address doesn't exist" in {
      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress("non existing"))).transact(database).runSyncUnsafe()

      // then
      cardanoAddressesInfo.size mustBe 0
    }

    "return last seen message id" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val lastSeenMessageId: Option[ConnectorMessageId] =
        CardanoAddressInfoDao.findLastSeenMessageId.transact(database).runSyncUnsafe()

      // then
      lastSeenMessageId mustBe Some(cardanoAddressInfo3.messageId)
    }
  }
}
