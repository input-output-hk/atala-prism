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

// sbt "project mirror" "testOnly *services.CardanoDeterministicWalletsServiceSpec"
class CardanoDeterministicWalletsServiceSpec
    extends PostgresRepositorySpec[Task]
    with MockitoSugar
    with MirrorFixtures {
  import ConnectorMessageFixtures._

  "registerWalletMessageProcessor" should {
    "upsert cardano wallet with addresses" in new CardanoDeterministicWalletsServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (addresses, processingResult) = (for {
        processingResult <- registerWalletMessageProcessor(cardanoRegisterWalletMessageMessage).get
        cardanoWallet <- CardanoWalletDao.findByName("wallet name").transact(database).map(_.get)
        addresses <- CardanoWalletAddressDao.findBy(cardanoWallet.id).transact(database)
      } yield (addresses, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Right[PrismError, Option[AtalaMessage]]]
      addresses.size mustBe 10
    }

    "return None if ReceivedMessage is not RegisterWalletMessageMessage" in new CardanoDeterministicWalletsServiceFixtures {
      registerWalletMessageProcessor(credentialMessage1) mustBe None
    }
  }

  trait CardanoDeterministicWalletsServiceFixtures {
    val cardanoConfig = CardanoConfig(CardanoNetwork.TestNet, 10)
    val cardanoAddressService = new CardanoAddressService("../target/mirror-binaries/cardano-address")
    val cardanoDeterministicWalletsService =
      new CardanoDeterministicWalletsService(database, cardanoAddressService, cardanoConfig)

    val registerWalletMessageProcessor = cardanoDeterministicWalletsService.registerWalletMessageProcessor
  }
}
