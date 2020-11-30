package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao}
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.models.Connection.PayIdName

import scala.concurrent.duration.DurationInt
import io.iohk.atala.mirror.stubs.NodeClientServiceStub
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import org.scalatest.Assertion

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
      val cardanoAddressInfoOption = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(paymentInformationMessage1).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
      } yield cardanoAddressInfoOption).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddressPayId1))
      cardanoAddressInfoOption.flatMap(_.payidVerifiedAddress) mustBe a[Some[_]]
    }

    "do not upsert cardano address if holder did and pay id name do not match" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      val paymentInformationWithWrongHolder =
        paymentInformation1.copy(payId =
          Some(PayID("wrongPayIdPrefix" + "$" + mirrorConfig.httpConfig.payIdHostAddress))
        )

      val messageWithWrongHolder =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongHolder))

      // when
      val cardanoAddressInfoOption = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(messageWithWrongHolder).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
      } yield cardanoAddressInfoOption).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption mustBe None
    }

    "do not upsert cardano address if host network is incorrect" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      val paymentInformationWithWrongNetwork =
        paymentInformation1.copy(payId = Some(PayID(connectionHolderDid2.value + "$" + "wrongNetwork")))

      val messageWithWrongNetwork =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongNetwork))

      // when
      val cardanoAddressInfoOption = (for {
        _ <- paymentInformationMessageProcessor.attemptProcessMessage(messageWithWrongNetwork).get
        cardanoAddressInfoOption <-
          CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddressPayId1)).transact(databaseTask)
      } yield cardanoAddressInfoOption).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption mustBe None
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

  "payIdNameRegistrationMessageProcessor" should {
    "register pay id name" in new CardanoAddressInfoServiceFixtures {
      testNameRegistration(
        connectionToUpdate = connection1,
        message = payIdNameRegistrationMessage1,
        updatedPayIdName = Some(PayIdName(payIdName1))
      )
    }

    "do register pay id name if it is not ascii compatible" in new CardanoAddressInfoServiceFixtures {
      val messageWithIncorrectName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage("ńćżóp"))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithIncorrectName,
        updatedPayIdName = None
      )
    }

    "do register pay id name if it is too short" in new CardanoAddressInfoServiceFixtures {
      val messageWithIncorrectName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage("aa"))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithIncorrectName,
        updatedPayIdName = None
      )
    }

    "do register pay id name if connection has already name" in new CardanoAddressInfoServiceFixtures {
      val incorrectMessage =
        payIdNameRegistrationMessage1.copy(connectionId = connectionId2.uuid.toString)

      testNameRegistration(
        connectionToUpdate = connection2,
        message = incorrectMessage,
        updatedPayIdName = Some(connectionPayIdName2)
      )
    }

    "do register pay id name if name has already been registered by other connection" in new CardanoAddressInfoServiceFixtures {
      val messageWithAlreadyRegisteredName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage(connectionPayIdName2.name))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithAlreadyRegisteredName,
        updatedPayIdName = None
      )
    }

    def testNameRegistration(
        connectionToUpdate: Connection,
        message: ReceivedMessage,
        updatedPayIdName: Option[PayIdName]
    ): Assertion = {
      // given
      val fixtures = new CardanoAddressInfoServiceFixtures {}
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      // when
      val updatedConnectionOption = (for {
        _ <- fixtures.payIdNameRegistrationMessageProcessor.attemptProcessMessage(message).get
        updatedConnection <- ConnectionDao.findByConnectionToken(connectionToUpdate.token).transact(databaseTask)
      } yield updatedConnection).runSyncUnsafe(1.minute)

      // then
      updatedConnectionOption.flatMap(_.payIdName) mustBe updatedPayIdName
    }

    "return None if ReceivedMessage is not PayIdNameRegistrationMessage" in new CardanoAddressInfoServiceFixtures {
      payIdNameRegistrationMessageProcessor.attemptProcessMessage(credentialMessage1) mustBe None
    }
  }

  "findPaymentInfo" should {
    "return connection and corresponding cardano addresses info by holderDID" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo = cardanoAddressInfoService
        .findPaymentInfoByHolderDid(connectionHolderDid2, cardanoNetwork2)
        .runSyncUnsafe()

      // then
      paymentInfo mustBe Some((connection2, List(cardanoAddressInfo2, cardanoAddressInfo3)))
    }

    "return None if connection with given holder did doesn't exists" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo =
        cardanoAddressInfoService
          .findPaymentInfoByHolderDid(DID.buildPrismDID("nonexisting"), cardanoNetwork2)
          .runSyncUnsafe()

      // then
      paymentInfo mustBe None
    }

    "return connection and corresponding cardano addresses info by payIdName" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo = cardanoAddressInfoService
        .findPaymentInfoByPayIdName(connectionPayIdName2, cardanoNetwork2)
        .runSyncUnsafe()

      // then
      paymentInfo mustBe Some((connection2, List(cardanoAddressInfo2, cardanoAddressInfo3)))
    }

    "return None if connection with given pay id name doesn't exist" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).unsafeRunSync()

      // when
      val paymentInfo =
        cardanoAddressInfoService
          .findPaymentInfoByPayIdName(PayIdName("nonExistingPayIdName"), cardanoNetwork2)
          .runSyncUnsafe()

      // then
      paymentInfo mustBe None
    }
  }

  trait CardanoAddressInfoServiceFixtures {
    val cardanoAddressInfoService =
      new CardanoAddressInfoService(databaseTask, mirrorConfig.httpConfig, defaultNodeClientStub)
    val cardanoAddressMessageProcessor = cardanoAddressInfoService.cardanoAddressInfoMessageProcessor
    val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor
    val payIdNameRegistrationMessageProcessor = cardanoAddressInfoService.payIdNameRegistrationMessageProcessor
  }
}
