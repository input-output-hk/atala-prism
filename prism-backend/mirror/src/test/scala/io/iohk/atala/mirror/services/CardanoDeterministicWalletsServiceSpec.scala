package io.iohk.atala.mirror.services

import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoWalletAddressDao, CardanoWalletDao}
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt
import io.iohk.atala.mirror.stubs.CardanoAddressServiceStub

// sbt "project mirror" "testOnly *services.CardanoDeterministicWalletsServiceSpec"
class CardanoDeterministicWalletsServiceSpec
    extends PostgresRepositorySpec[Task]
    with MockitoSugar
    with MirrorFixtures {
  import CardanoWalletFixtures._
  import ConnectorMessageFixtures._

  "registerWalletMessageProcessor" should {
    "upsert cardano wallet with addresses when all address has not been used" in new CardanoDeterministicWalletsServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoDBSyncFixtures.createDbSyncSchema(database)
      } yield ()).runSyncUnsafe()

      // when
      val (cardanoWallet, addresses, processingResult) = (for {
        processingResult <- registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage).get
        cardanoWallet <- CardanoWalletDao.findByName("wallet name").transact(database).map(_.get)
        addresses <- CardanoWalletAddressDao.findBy(cardanoWallet.id).transact(database)
      } yield (cardanoWallet, addresses, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Right[PrismError, Option[AtalaMessage]]]
      val message = processingResult.toOption.value.value
      message.getMirrorMessage.getWalletRegistered.name mustBe cardanoWalletName
      message.getMirrorMessage.getWalletRegistered.extendedPublicKey mustBe cardanoExtendedPublicKey
      addresses.size mustBe 10
      cardanoWallet.lastGeneratedNo mustBe (minAddressesCount - 1)
      cardanoWallet.lastUsedNo mustBe None
    }

    "upsert cardano wallet with addresses when 15 addresses has been used" in new CardanoDeterministicWalletsServiceFixtures {
      // given
      val usedAddressesCount = 15
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoDBSyncFixtures.createDbSyncSchema(database)
        _ <- CardanoDBSyncFixtures.insert(usedAddressesCount, cardanoAddressServiceStub, database)
      } yield ()).runSyncUnsafe()

      // when
      val (cardanoWallet, addresses, processingResult) = (for {
        processingResult <- registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage).get
        cardanoWallet <- CardanoWalletDao.findByName("wallet name").transact(database).map(_.get)
        addresses <- CardanoWalletAddressDao.findBy(cardanoWallet.id).transact(database)
      } yield (cardanoWallet, addresses, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Right[PrismError, Option[AtalaMessage]]]
      addresses.size mustBe minAddressesCount * 3
      cardanoWallet.lastGeneratedNo mustBe addresses.size - 1
      cardanoWallet.lastUsedNo mustBe Some(usedAddressesCount - 1)
    }

    "return None if ReceivedMessage is not RegisterWalletMessageMessage" in new CardanoDeterministicWalletsServiceFixtures {
      registerWalletMessageProcessor(credentialMessage1) mustBe None
      registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage) mustBe a[Some[_]]
    }

    "handle duplicate wallets without throwing exception" in new CardanoDeterministicWalletsServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoDBSyncFixtures.createDbSyncSchema(database)
      } yield ()).runSyncUnsafe()

      // when
      val (processingResult1, processingResult2) = (for {
        processingResult1 <- registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage).get
        processingResult2 <- registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage).get
      } yield (processingResult1, processingResult2)).runSyncUnsafe(1.minute)

      // then
      processingResult1 mustBe an[Right[PrismError, Option[AtalaMessage]]]
      processingResult2 mustBe Left(
        CardanoDeterministicWalletsService.CardanoWalletExists
      )
    }
  }

  trait CardanoDeterministicWalletsServiceFixtures {
    val cardanoAddressServiceStub = new CardanoAddressServiceStub()
    val cardanoDeterministicWalletsService =
      new CardanoDeterministicWalletsService(database, database, cardanoAddressServiceStub, cardanoConfig)

    val registerWalletMessageProcessor = cardanoDeterministicWalletsService.registerWalletMessageProcessor
  }
}
