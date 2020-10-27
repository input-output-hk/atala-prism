package io.iohk.atala.mirror.services

import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.CardanoAddressInfoDao
import io.iohk.atala.mirror.models.CardanoAddressInfo.CardanoAddress
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import org.mockito.scalatest.MockitoSugar
import monix.execution.Scheduler.Implicits.global
import doobie.implicits._
import io.iohk.atala.mirror.models.DID

import scala.concurrent.duration.DurationInt

// sbt "project mirror" "testOnly *services.CardanoAddressInfoServiceSpec"
class CardanoAddressInfoServiceSpec extends PostgresRepositorySpec with MockitoSugar with MirrorFixtures {
  import ConnectorMessageFixtures._, ConnectionFixtures._, CardanoAddressInfoFixtures._

  "cardanoAddressesMessageProcessor" should {
    "upsert cardano address" in new CardanoAddressInfoServiceFixtures {
      // given
      ConnectionFixtures.insertAll(databaseTask).runSyncUnsafe()

      // when
      val cardanoAddressInfoOption = (for {
        _ <- messageProcessor.attemptProcessMessage(cardanoAddressInfoMessage1).get
        cardanoAddress <- CardanoAddressInfoDao.findBy(CardanoAddress(cardanoAddress1)).transact(databaseTask)
      } yield cardanoAddress).runSyncUnsafe(1.minute)

      // then
      cardanoAddressInfoOption.map(_.cardanoAddress) mustBe Some(CardanoAddress(cardanoAddress1))
    }

    "return None if ReceivedMessage is not CardanoAddressMessage" in new CardanoAddressInfoServiceFixtures {
      messageProcessor.attemptProcessMessage(credentialMessage1) mustBe None
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
      val paymentInfo = cardanoAddressInfoService.findPaymentInfo(connectionHolderDid2, cardanoNetwork2).runSyncUnsafe()

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
        cardanoAddressInfoService.findPaymentInfo(DID("non existing did"), cardanoNetwork2).runSyncUnsafe()

      // then
      paymentInfo mustBe None
    }
  }

  trait CardanoAddressInfoServiceFixtures {
    val cardanoAddressInfoService = new CardanoAddressInfoService(databaseTask)
    val messageProcessor = cardanoAddressInfoService.cardanoAddressInfoMessageProcessor
  }
}
