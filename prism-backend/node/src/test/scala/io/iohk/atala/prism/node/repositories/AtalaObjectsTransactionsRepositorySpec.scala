package io.iohk.atala.prism.node.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.AtalaWithPostgresSpec
import io.iohk.atala.prism.node.DataPreparation
import io.iohk.atala.prism.node.services.BlockProcessingServiceSpec
import io.iohk.atala.prism.node.services.models.AtalaObjectNotification
import io.iohk.atala.prism.utils.IOUtils._
import tofu.logging.Logs

class AtalaObjectsTransactionsRepositorySpec extends AtalaWithPostgresSpec {
  private val testLogs = Logs.sync[IO, IO]
  private lazy implicit val repository: AtalaObjectsTransactionsRepository[IO] =
    AtalaObjectsTransactionsRepository.unsafe(database, testLogs)

  private val signedOperationDummy =
    BlockProcessingServiceSpec.signedCreateDidOperation
  private val blockDummy = DataPreparation.createBlock(signedOperationDummy)
  private val objectDummy = DataPreparation.createAtalaObject(blockDummy)

  private val atalaObjectNotificationDummy =
    AtalaObjectNotification(objectDummy, DataPreparation.dummyTransactionInfo)

  "AtalaObjectsTransactionsRepository.setObjectTransactionDetails" should {
    "not schedule new operations" in {
      val objectInfo = repository
        .setObjectTransactionDetails(atalaObjectNotificationDummy)
        .unsafeToFuture()
        .futureValue
      objectInfo.isDefined must be(true)

      val notPublishedObjects = repository.getNotPublishedObjects
        .unsafeToFuture()
        .futureValue
        .toOption
        .get
      notPublishedObjects.isEmpty must be(true)
    }
  }
}
