package io.iohk.node.services

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.repositories.PostgresRepositorySpec
import io.iohk.node.cardano.CardanoClient
import io.iohk.node.cardano.dbsync.CardanoDbSyncClient
import io.iohk.node.cardano.dbsync.repositories.CardanoBlockRepository
import io.iohk.node.cardano.models._
import io.iohk.node.cardano.wallet.CardanoWalletApiClient
import io.iohk.node.cardano.wallet.testing.FakeCardanoWalletApiClient
import io.iohk.node.services.models.{AtalaObjectUpdate, ObjectHandler}
import org.scalatest.OptionValues._

import scala.concurrent.Future
import scala.concurrent.duration._

class CardanoLedgerServiceSpec extends PostgresRepositorySpec {
  implicit val pc: PatienceConfig = PatienceConfig(20.seconds, 50.millis)

  val walletId: WalletId = WalletId.from("bf098c001609ad7b76a0239e27f2a6bf9f09fd71").value
  val walletPassphrase = "Secure Passphrase"
  val paymentAddress: Address = Address("2cWKMJemoBakZBR9TG2YAmxxtJpyvBqv31yWuHjUWpjbc24XbxiLytuzxSdyMtrbCfGmb")

  "publishReference" should {
    val reference = SHA256Digest.compute("AtalaObjectReference".getBytes)
    val expectedWalletApiPath = s"v2/byron-wallets/$walletId/transactions"

    "publish a reference and notify immediately" in {
      var notifiedAtalaObjectUpdate: Option[AtalaObjectUpdate] = None
      val cardanoWalletApiClient = FakeCardanoWalletApiClient.Success(
        expectedWalletApiPath,
        readResource("publishReference_cardanoWalletApiRequest.json"),
        readResource("publishReference_success_cardanoWalletApiResponse.json")
      )
      val cardanoLedgerService = createCardanoLedgerService(
        cardanoWalletApiClient,
        (atalaObject, _) => {
          notifiedAtalaObjectUpdate = Some(atalaObject)
          Future.successful(())
        }
      )

      cardanoLedgerService.publishReference(reference).futureValue

      notifiedAtalaObjectUpdate.value must be(AtalaObjectUpdate.Reference(reference))
    }

    "fail with invalid transaction" in {
      val cardanoWalletApiClient =
        FakeCardanoWalletApiClient.Fail(
          expectedWalletApiPath,
          readResource("publishReference_cardanoWalletApiRequest.json"),
          "internal",
          "Internal error"
        )
      val cardanoLedgerService = createCardanoLedgerService(cardanoWalletApiClient, (_, _) => Future.unit)

      val error = intercept[RuntimeException] {
        cardanoLedgerService.publishReference(reference).futureValue
      }

      error.getCause.getMessage must be("FATAL: Error during publishing reference: InvalidTransaction")
    }
  }

  def createCardanoLedgerService(
      cardanoWalletApiClient: CardanoWalletApiClient,
      onNewObject: ObjectHandler
  ): CardanoLedgerService = {
    val cardanoClient = createCardanoClient(cardanoWalletApiClient)
    new CardanoLedgerService(
      walletId,
      walletPassphrase,
      paymentAddress,
      cardanoClient,
      onNewObject
    )
  }

  def createCardanoClient(cardanoWalletApiClient: CardanoWalletApiClient): CardanoClient = {
    new CardanoClient(new CardanoDbSyncClient(new CardanoBlockRepository(database)), cardanoWalletApiClient)
  }

  private def readResource(resource: String): String = {
    try {
      scala.io.Source.fromResource(s"services/cardano/$resource").mkString
    } catch {
      case _: Throwable => throw new RuntimeException(s"Resource $resource not found")
    }
  }
}
