package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, PayIdMessageDao}
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.iohk.atala.mirror.models.ConnectorMessageId

import scala.concurrent.duration.DurationInt
import io.circe.parser._
import io.iohk.atala.mirror.stubs.NodeClientServiceStub
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.mirror.payid.implicits._

// sbt "project mirror" "testOnly *services.CardanoAddressInfoServiceSpec"
class CardanoAddressInfoServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import ConnectorMessageFixtures._, ConnectionFixtures._, CardanoAddressInfoFixtures._, PayIdFixtures._,
  CredentialFixtures._

  "cardanoAddressesMessageProcessor" should {
    "upsert cardano address" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      // when
      val cardanoAddressInfoOption = (for {
        _ <- cardanoAddressMessageProcessor.attemptProcessMessage(cardanoAddressInfoMessage1).get
        cardanoAddress <- CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddress1)).transact(databaseTask)
      } yield cardanoAddress).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddress1))
    }

    "return None if ReceivedMessage is not CardanoAddressMessage" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressMessageProcessor.attemptProcessMessage(credentialMessage1) mustBe None
    }
  }

  "payIdMessageProcessor" should {
    "upsert cardano address" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      // when
      val (cardanoAddressInfoOption, payIdMessageOption) = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(paymentInformationMessage1).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
        payIdMessageOption <-
          PayIdMessageDao.findBy(ConnectorMessageId(paymentInformationMessage1.id)).transact(databaseTask)
      } yield (cardanoAddressInfoOption, payIdMessageOption)).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddressPayId1))
      payIdMessageOption
        .map(_.rawPaymentInformation.raw)
        .flatMap(rawPaymentInformation => decode[PaymentInformation](rawPaymentInformation).toOption) mustBe Some(
        paymentInformation1
      )
    }

    "do not upsert cardano address if holder did is incorrect" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      val paymentInformationWithWrongHolder =
        paymentInformation1.copy(payId = Some(PayID("wrongHolderDid" + "$" + mirrorConfig.httpConfig.payIdHostAddress)))

      val messageWithWrongHolder =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongHolder))

      // when
      val (cardanoAddressInfoOption, payIdMessageOption) = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(messageWithWrongHolder).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
        payIdMessageOption <-
          PayIdMessageDao.findBy(ConnectorMessageId(paymentInformationMessage1.id)).transact(databaseTask)
      } yield (cardanoAddressInfoOption, payIdMessageOption)).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption mustBe None
      payIdMessageOption mustBe None
    }

    "do not upsert cardano address if host network is incorrect" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      val paymentInformationWithWrongNetwork =
        paymentInformation1.copy(payId = Some(PayID(connectionHolderDid2.value + "$" + "wrongNetwork")))

      val messageWithWrongNetwork =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongNetwork))

      // when
      val (cardanoAddressInfoOption, payIdMessageOption) = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(messageWithWrongNetwork).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
        payIdMessageOption <-
          PayIdMessageDao.findBy(ConnectorMessageId(paymentInformationMessage1.id)).transact(databaseTask)
      } yield (cardanoAddressInfoOption, payIdMessageOption)).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption mustBe None
      payIdMessageOption mustBe None
    }

    "do not upsert cardano address if address signature cannot be verified (key doesn't exist)" in {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      val nodeClientStub = new NodeClientServiceStub()
      val cardanoAddressInfoService =
        new CardanoAddressInfoService(databaseTask, mirrorConfig.httpConfig, nodeClientStub)
      val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor

      // when
      val cardanoAddressInfoOption = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(paymentInformationMessage1).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
      } yield cardanoAddressInfoOption).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe None
    }

    "return None if ReceivedMessage is not PaymentInformationMessage" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressMessageProcessor.attemptProcessMessage(credentialMessage1) mustBe None
    }
  }

  "findPaymentInfo" should {
    "return connection and corresponding cardano addresses info" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo = cardanoAddressInfoService
        .findPaymentInfo(connectionHolderDid2, cardanoNetwork2)
        .runSyncUnsafe()

      // then
      paymentInfo mustBe Some((connection2, List(cardanoAddressInfo2, cardanoAddressInfo3)))
    }

    "return None if connection with given holder did" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo =
        cardanoAddressInfoService.findPaymentInfo(DID.buildPrismDID("nonexisting"), cardanoNetwork2).runSyncUnsafe()

      // then
      paymentInfo mustBe None
    }
  }

  trait CardanoAddressInfoServiceFixtures {
    val cardanoAddressInfoService =
      new CardanoAddressInfoService(databaseTask, mirrorConfig.httpConfig, defaultNodeClientStub)
    val cardanoAddressMessageProcessor = cardanoAddressInfoService.cardanoAddressInfoMessageProcessor
    val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor
  }
}
