package io.iohk.atala.mirror.services

import cats.data.NonEmptyList
import doobie.implicits._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoAddressInfoDao, ConnectionDao}
import io.iohk.atala.mirror.models.CardanoAddress
import io.iohk.atala.mirror.models.Connection
import io.iohk.atala.mirror.models.Connection.PayIdName
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.mirror.payid._
import io.iohk.atala.prism.protos.connector_models.ReceivedMessage
import io.iohk.atala.prism.protos.credential_models.AtalaMessage
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.stubs.NodeClientServiceStub
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.mockito.scalatest.MockitoSugar
import org.scalatest.Assertion
import CardanoAddressInfoService._

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.CardanoAddressInfoServiceSpec"
class CardanoAddressInfoServiceSpec extends PostgresRepositorySpec[Task] with MockitoSugar with MirrorFixtures {
  import CardanoAddressInfoFixtures._
  import ConnectionFixtures._
  import ConnectorMessageFixtures._
  import CredentialFixtures._
  import PayIdFixtures._

  "cardanoAddressesMessageProcessor" should {
    "upsert cardano address" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (cardanoAddressInfoOption, processingResult) = (for {
        processingResult <- cardanoAddressMessageProcessor(cardanoAddressInfoMessage1).get
        cardanoAddress <-
          CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(cardanoAddress1))).transact(database)
      } yield (cardanoAddress.headOption, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Right[PrismError, Some[AtalaMessage]]]
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddress1))
    }

    "return None if ReceivedMessage is not CardanoAddressMessage" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressMessageProcessor(credentialMessage1) mustBe None
    }
  }

  "payIdMessageProcessor" should {
    "upsert cardano address" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (cardanoAddressInfoOption, processingResult) = (for {
        processingResult <- paymentInformationMessageProcessor(paymentInformationMessage1).get
        cardanoAddresses <-
          CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(cardanoAddressPayId1))).transact(database)
      } yield (cardanoAddresses.headOption, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Right[PrismError, Some[AtalaMessage]]]
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddressPayId1))
      cardanoAddressInfoOption.flatMap(_.payidVerifiedAddress) mustBe a[Some[_]]
    }

    "do not upsert cardano address if holder did and pay id name do not match" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      val paymentInformationWithWrongHolder =
        paymentInformation1.copy(payId =
          Some(PayID("wrongPayIdPrefix" + "$" + mirrorConfig.httpConfig.payIdHostAddress))
        )

      val messageWithWrongHolder =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongHolder))

      // when
      val (cardanoAddressInfoOption, processingResult) = (for {
        processingResult <- paymentInformationMessageProcessor(messageWithWrongHolder).get
        cardanoAddresses <-
          CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(cardanoAddressPayId1))).transact(database)
      } yield (cardanoAddresses.headOption, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Left[ConnectionDoesNotContainPayIdInfo, Option[AtalaMessage]]]
      cardanoAddressInfoOption mustBe None
    }

    "do not upsert cardano address if host network is incorrect" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      val paymentInformationWithWrongNetwork =
        paymentInformation1.copy(payId = Some(PayID(connectionHolderDid2.value + "$" + "wrongNetwork")))

      val messageWithWrongNetwork =
        paymentInformationMessage1.copy(message = paymentInformationToAtalaMessage(paymentInformationWithWrongNetwork))

      // when
      val (cardanoAddressInfoOption, processingResult) = (for {
        processingResult <- paymentInformationMessageProcessor(messageWithWrongNetwork).get
        cardanoAddresses <-
          CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(cardanoAddressPayId1))).transact(database)
      } yield (cardanoAddresses.headOption, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Left[PayIdHostAddressDoesNotMach, Some[AtalaMessage]]]
      cardanoAddressInfoOption mustBe None
    }

    "do not upsert cardano address if address signature cannot be verified (key doesn't exist)" in {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      val nodeClientStub = new NodeClientServiceStub()
      val cardanoAddressInfoService =
        new CardanoAddressInfoService(database, mirrorConfig.httpConfig, nodeClientStub)
      val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor

      // when
      val (cardanoAddressInfoOption, processingResult) = (for {
        processingResult <- paymentInformationMessageProcessor(paymentInformationMessage1).get
        cardanoAddresses <-
          CardanoAddressInfoDao.findBy(NonEmptyList.of(CardanoAddress(cardanoAddressPayId1))).transact(database)
      } yield (cardanoAddresses.headOption, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResult mustBe an[Left[VerifiedAddressesWithNotValidSignature, Some[AtalaMessage]]]
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe None
    }

    "return None if ReceivedMessage is not PaymentInformationMessage" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressMessageProcessor(credentialMessage1) mustBe None
    }
  }

  "payIdNameRegistrationMessageProcessor" should {
    "register pay id name" in new CardanoAddressInfoServiceFixtures {
      testNameRegistration(
        connectionToUpdate = connection1,
        message = payIdNameRegistrationMessage1,
        updatedPayIdName = Some(PayIdName(payIdName1)),
        processingResultAssertion = _ mustBe an[Right[PrismError, Some[AtalaMessage]]]
      )
    }

    "do not register pay id name if it is not ascii compatible" in new CardanoAddressInfoServiceFixtures {
      val messageWithIncorrectName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage("ńćżóp"))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithIncorrectName,
        updatedPayIdName = None,
        processingResultAssertion = _ mustBe an[Left[PayIdNameNotAllowedCharacters, Some[AtalaMessage]]]
      )
    }

    "do not register pay id name if it is too short" in new CardanoAddressInfoServiceFixtures {
      val messageWithIncorrectName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage("aa"))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithIncorrectName,
        updatedPayIdName = None,
        processingResultAssertion = _ mustBe an[Left[PayIdNameIncorrectLength, Some[AtalaMessage]]]
      )
    }

    "do not register pay id name if connection has already name" in new CardanoAddressInfoServiceFixtures {
      val incorrectMessage =
        payIdNameRegistrationMessage1.copy(connectionId = connectionId2.uuid.toString)

      testNameRegistration(
        connectionToUpdate = connection2,
        message = incorrectMessage,
        updatedPayIdName = Some(connectionPayIdName2),
        processingResultAssertion = _ mustBe an[Left[ConnectionHasAlreadyRegisteredPayIdName, Some[AtalaMessage]]]
      )
    }

    "do not register pay id name if name has already been registered by other connection" in new CardanoAddressInfoServiceFixtures {
      val messageWithAlreadyRegisteredName =
        payIdNameRegistrationMessage1.copy(message = payIdNameRegistrationToAtalaMessage(connectionPayIdName2.name))

      testNameRegistration(
        connectionToUpdate = connection1,
        message = messageWithAlreadyRegisteredName,
        updatedPayIdName = None,
        processingResultAssertion = _ mustBe an[Right[PrismError, Some[AtalaMessage]]]
      )
    }

    def testNameRegistration(
        connectionToUpdate: Connection,
        message: ReceivedMessage,
        updatedPayIdName: Option[PayIdName],
        processingResultAssertion: Either[PrismError, Option[AtalaMessage]] => Assertion
    ): Assertion = {
      // given
      val fixtures = new CardanoAddressInfoServiceFixtures {}
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (updatedConnectionOption, processingResult) = (for {
        processingResult <- fixtures.payIdNameRegistrationMessageProcessor(message).get
        updatedConnection <- ConnectionDao.findByConnectionToken(connectionToUpdate.token).transact(database)
      } yield (updatedConnection, processingResult)).runSyncUnsafe(1.minute)

      // then
      processingResultAssertion(processingResult)
      updatedConnectionOption.flatMap(_.payIdName) mustBe updatedPayIdName
    }

    "return None if ReceivedMessage is not PayIdNameRegistrationMessage" in new CardanoAddressInfoServiceFixtures {
      payIdNameRegistrationMessageProcessor(credentialMessage1) mustBe None
    }
  }

  "findPaymentInfo" should {
    "return connection and corresponding cardano addresses info by holderDID" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

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
      } yield ()).runSyncUnsafe()

      // when
      val paymentInfo = cardanoAddressInfoService
        .findPaymentInfoByHolderDid(newDID(), cardanoNetwork2)
        .runSyncUnsafe()

      // then
      paymentInfo mustBe None
    }

    "return connection and corresponding cardano addresses info by payIdName" in new CardanoAddressInfoServiceFixtures {
      // given
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

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
      } yield ()).runSyncUnsafe()

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
      new CardanoAddressInfoService(database, mirrorConfig.httpConfig, defaultNodeClientStub)
    val cardanoAddressMessageProcessor = cardanoAddressInfoService.cardanoAddressInfoMessageProcessor
    val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor
    val payIdNameRegistrationMessageProcessor = cardanoAddressInfoService.payIdNameRegistrationMessageProcessor
  }
}
