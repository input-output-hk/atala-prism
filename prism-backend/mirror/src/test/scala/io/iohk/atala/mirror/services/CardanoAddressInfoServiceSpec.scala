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
import org.scalatest.OptionValues._

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.CardanoAddressInfoServiceSpec"
class CardanoAddressInfoServiceSpec extends PostgresRepositorySpec[Task] with MockitoSugar with MirrorFixtures {
  import CardanoAddressInfoFixtures._
  import ConnectionFixtures._
  import ConnectorMessageFixtures._
  import CredentialFixtures._
  import PayIdFixtures._
  import CardanoWalletFixtures._

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
      val message = processingResult.toOption.value.value
      message.getMirrorMessage.getAddressRegisteredMessage.cardanoAddress mustBe cardanoAddress1
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddress1))
    }

    "handle duplicate addreses without throwing exception" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (processingResult1, processingResult2) = (for {
        processingResult1 <- cardanoAddressMessageProcessor(cardanoAddressInfoMessage1).get
        processingResult2 <- cardanoAddressMessageProcessor(cardanoAddressInfoMessage1).get
      } yield (processingResult1, processingResult2)).runSyncUnsafe(1.minute)

      // then
      processingResult1 mustBe an[Right[PrismError, Some[AtalaMessage]]]
      processingResult2 mustBe an[Left[CardanoAddressAlreadyExist, Some[AtalaMessage]]]
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

    "handle duplicate addreses without throwing exception" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(database).runSyncUnsafe()

      // when
      val (processingResult1, processingResult2) = (for {
        processingResult1 <- paymentInformationMessageProcessor(paymentInformationMessage1).get
        processingResult2 <- paymentInformationMessageProcessor(paymentInformationMessage1).get
      } yield (processingResult1, processingResult2)).runSyncUnsafe(1.minute)

      // then
      processingResult1 mustBe an[Right[PrismError, Some[AtalaMessage]]]
      processingResult2 mustBe an[Left[CardanoAddressAlreadyExist, Some[AtalaMessage]]]
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
        processingResultAssertion = { result =>
          result mustBe an[Right[PrismError, Some[AtalaMessage]]]
          val message = result.toOption.value.value
          message.getMirrorMessage.getPayIdNameRegisteredMessage.name mustBe payIdName1
        }
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

  "checkPayIdNameAvailabilityMessageProcessor" should {
    "return true when name is available" in new CardanoAddressInfoServiceFixtures {
      val message = makeReceivedMessage(message = checkPayIdNameToAtalaMessage("nonExistingPayIdName"))

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        result <- checkPayIdNameAvailabilityMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      result.toOption.flatten.map(_.getMirrorMessage.getCheckPayIdNameAvailabilityResponse.available) mustBe Some(true)
    }

    "return false when name is not available" in new CardanoAddressInfoServiceFixtures {
      val message = makeReceivedMessage(message = checkPayIdNameToAtalaMessage(connectionPayIdName2.name))

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        result <- checkPayIdNameAvailabilityMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      result.toOption.flatten.map(_.getMirrorMessage.getCheckPayIdNameAvailabilityResponse.available) mustBe Some(false)
    }
  }

  "verifyPayIdString" should {
    "return unit when payId name is correct" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressInfoService.verifyPayIdString("someCorrectName") mustBe Right(())
    }

    "return error when payId name is to short" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressInfoService.verifyPayIdString("a") mustBe an[Left[PayIdNameIncorrectLength, Unit]]
    }

    "return error when payId name is to long" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressInfoService
        .verifyPayIdString(1.to(61).map(_ => "a").mkString("")) mustBe an[Left[PayIdNameIncorrectLength, Unit]]
    }

    "return error when payId name contains illegal characters" in new CardanoAddressInfoServiceFixtures {
      cardanoAddressInfoService.verifyPayIdString("ąńÓpeaddd") mustBe an[Left[PayIdNameNotAllowedCharacters, Unit]]
    }
  }

  "getPayIdNameMessageProcessor" should {
    "return payIdName when payIdName is registered for connection" in new CardanoAddressInfoServiceFixtures {
      val message =
        makeReceivedMessage(connectionId = connectionId2.uuid.toString, message = getPayIdNameToAtalaMessage)

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        result <- getPayIdNameMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      result.toOption.flatten.map(_.getMirrorMessage.getGetPayIdNameResponse.payIdName) mustBe Some(
        connectionPayIdName2.name
      )
    }

    "return empty string when payIdName is not registered for connection" in new CardanoAddressInfoServiceFixtures {
      val message = makeReceivedMessage(message = getPayIdNameToAtalaMessage)

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        result <- getPayIdNameMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      result.toOption.flatten.map(_.getMirrorMessage.getGetPayIdNameResponse.payIdName) mustBe Some("")
    }
  }

  "getPayIdAddressesMessageProcessor" should {
    "return manually registered cardano addresses for connection" in new CardanoAddressInfoServiceFixtures {
      val message =
        makeReceivedMessage(connectionId = connectionId2.uuid.toString, message = getPayIdAddressesToAtalaMessage)

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoAddressInfoFixtures.insertAll(database)
        _ <- CardanoWalletFixtures.insertAll(database)
        result <- getPayIdAddressesMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      val getGetPayIdAddressesResponse = result.toOption.flatten
        .map(_.getMirrorMessage.getGetPayIdAddressesResponse)
        .value

      val manuallyRegisteredCardanoAddresses = getGetPayIdAddressesResponse.manuallyRegisteredCardanoAddresses

      manuallyRegisteredCardanoAddresses.map(cardanoAddress =>
        (cardanoAddress.address, cardanoAddress.registrationDate.map(_.seconds))
      ) mustBe
        Seq(
          (cardanoAddressInfo2.cardanoAddress.value, Some(cardanoAddressInfo2.registrationDate.date.getEpochSecond)),
          (cardanoAddressInfo3.cardanoAddress.value, Some(cardanoAddressInfo3.registrationDate.date.getEpochSecond))
        )

      val generatedCardanoAddresses = getGetPayIdAddressesResponse.generatedCardanoAddresses

      generatedCardanoAddresses.head.address mustBe cardanoWalletAddress1.address.value
      generatedCardanoAddresses(1).address mustBe cardanoWalletAddress2.address.value
    }
  }

  "getRegisteredWalletsMessageProcessor" should {
    "return manually registered wallets" in new CardanoAddressInfoServiceFixtures {
      val message =
        makeReceivedMessage(connectionId = connectionId2.uuid.toString, message = getRegisteredWalletsToAtalaMessage)

      val result = (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoWalletFixtures.insertAll(database)
        result <- getRegisteredWalletsMessageProcessor(message).get
      } yield result).runSyncUnsafe()

      val getRegisteredWalletsResponse = result.toOption.flatten
        .map(_.getMirrorMessage.getGetRegisteredWalletsResponse)
        .value

      val wallets = getRegisteredWalletsResponse.wallets

      wallets.map(wallet => (wallet.id, wallet.name, wallet.extendedPublicKey)) mustBe
        Seq(
          (cardanoWallet1.id.uuid.toString, cardanoWallet1.name.getOrElse(""), cardanoWallet1.extendedPublicKey),
          (cardanoWallet2.id.uuid.toString, cardanoWallet2.name.getOrElse(""), cardanoWallet2.extendedPublicKey)
        )
    }
  }

  trait CardanoAddressInfoServiceFixtures {
    val cardanoAddressInfoService =
      new CardanoAddressInfoService(database, mirrorConfig.httpConfig, defaultNodeClientStub)
    val cardanoAddressMessageProcessor = cardanoAddressInfoService.cardanoAddressInfoMessageProcessor
    val paymentInformationMessageProcessor = cardanoAddressInfoService.payIdMessageProcessor
    val payIdNameRegistrationMessageProcessor = cardanoAddressInfoService.payIdNameRegistrationMessageProcessor
    val checkPayIdNameAvailabilityMessageProcessor =
      cardanoAddressInfoService.checkPayIdNameAvailabilityMessageProcessor
    val getPayIdNameMessageProcessor = cardanoAddressInfoService.getPayIdNameMessageProcessor
    val getPayIdAddressesMessageProcessor = cardanoAddressInfoService.getPayIdAddressesMessageProcessor
    val getRegisteredWalletsMessageProcessor = cardanoAddressInfoService.getRegisteredWalletsMessageProcessor
  }
}
