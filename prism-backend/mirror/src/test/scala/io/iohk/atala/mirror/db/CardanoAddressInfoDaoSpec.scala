package io.iohk.atala.mirror.db

import scala.concurrent.duration._
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.prism.models.ConnectorMessageId

// sbt "project mirror" "testOnly *db.CardanoAddressInfoDaoSpec"
class CardanoAddressInfoDaoSpec extends PostgresRepositorySpec with MirrorFixtures {
  import ConnectionFixtures._, CardanoAddressInfoFixtures._

  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 500.millis)

  "CardanoAddressDao" should {
    "insert single cardano address into the db" in {
      // when
      val resultCount = (for {
        _ <- ConnectionDao.insert(connection1)
        resultCount <- CardanoAddressInfoDao.insert(cardanoAddressInfo1)
      } yield resultCount)
        .transact(database)
        .unsafeRunSync()

      // then
      resultCount mustBe 1
    }

    "return cardano address by address" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao.findBy(cardanoAddressInfo1.cardanoAddress).transact(database).unsafeRunSync()

      // then
      cardanoAddressesInfo mustBe Some(cardanoAddressInfo1)
    }

    "return cardano addresses by connection token and cardano network" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao
          .findBy(connection1.token, cardanoAddressInfo1.cardanoNetwork)
          .transact(database)
          .unsafeRunSync()

      // then
      cardanoAddressesInfo mustBe List(cardanoAddressInfo1)
    }

    "return none if a cardano address doesn't exist" in {
      // when
      val cardanoAddressesInfo =
        CardanoAddressInfoDao.findBy(CardanoAddress("non existing")).transact(database).unsafeRunSync()

      // then
      cardanoAddressesInfo.size mustBe 0
    }

    "return last seen message id" in {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val lastSeenMessageId: Option[ConnectorMessageId] =
        CardanoAddressInfoDao.findLastSeenMessageId.transact(database).unsafeRunSync()

      // then
      lastSeenMessageId mustBe Some(cardanoAddressInfo3.messageId)
    }
  }
}
