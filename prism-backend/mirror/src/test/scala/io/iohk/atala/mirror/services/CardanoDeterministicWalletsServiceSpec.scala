package io.iohk.atala.mirror.services

import scala.concurrent.duration.DurationInt
import monix.eval.Task
import org.mockito.scalatest.MockitoSugar
import io.iohk.atala.mirror.db.CardanoWalletDao
import io.iohk.atala.mirror.db.CardanoWalletAddressDao
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.prism.errors.PrismError
import doobie.implicits._
import monix.execution.Scheduler.Implicits.global
import io.iohk.atala.mirror.config.CardanoConfig
import io.iohk.atala.mirror.config.CardanoNetwork
import org.scalatest.OptionValues._

// sbt "project mirror" "testOnly *services.CardanoDeterministicWalletsServiceSpec"
class CardanoDeterministicWalletsServiceSpec
    extends PostgresRepositorySpec[Task]
    with MockitoSugar
    with MirrorFixtures {
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
        _ <- CardanoDBSyncFixtures.insert(usedAddressesCount, cardanoAddressService, database)
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
    }
  }

  trait CardanoDeterministicWalletsServiceFixtures {
    val minAddressesCount = 10
    val cardanoConfig = CardanoConfig(CardanoNetwork.TestNet, minAddressesCount)
    val cardanoAddressService = new CardanoAddressService("../target/mirror-binaries/cardano-address")
    val cardanoDeterministicWalletsService =
      new CardanoDeterministicWalletsService(database, database, cardanoAddressService, cardanoConfig)

    val registerWalletMessageProcessor = cardanoDeterministicWalletsService.registerWalletMessageProcessor
  }
}
