package io.iohk.atala.mirror.task.lease.system.processors

import io.circe.syntax._
import io.iohk.atala.mirror.MirrorFixtures
import io.iohk.atala.mirror.db.{CardanoWalletAddressDao, CardanoWalletDao}
import io.iohk.atala.mirror.models.CardanoBlockId
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskState
import io.iohk.atala.mirror.task.lease.system.data.WatchCardanoBlockchainAddressesStateData
import io.iohk.atala.prism.repositories.PostgresRepositorySpec
import io.iohk.atala.prism.task.lease.system.ProcessingTaskResult._
import io.iohk.atala.prism.task.lease.system.{ProcessingTaskData, ProcessingTaskFixtures}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.mockito.scalatest.MockitoSugar
import org.scalatest.OptionValues._
import doobie.implicits._
import io.iohk.atala.prism.stubs.ProcessingTaskServiceStub

import scala.concurrent.duration._

class WatchCardanoBlockchainAddressesProcessorSpec
    extends PostgresRepositorySpec[Task]
    with MockitoSugar
    with MirrorFixtures {
  import CardanoWalletFixtures._
  import CardanoDBSyncFixtures._

  "WatchCardanoBlockchainAddressesProcessor" should {
    "process new blocks by updating cardano wallet and addresses" in new WatchCardanoBlockchainAddressesProcessorFixtures {
      // given
      val usedAddressesCount = 15
      (for {
        _ <- ConnectionFixtures.insertAll(database)
        _ <- CardanoDBSyncFixtures.createDbSyncSchema(database)
        _ <- CardanoDBSyncFixtures.insert(usedAddressesCount, cardanoAddressServiceStub, database)
        _ <- CardanoWalletFixtures.insertAll(database)
      } yield ()).runSyncUnsafe()

      // when
      val (cardanoWallet, addresses, result) = (for {
        processingResult <- processor.process(processingTask, workerNumber)
        cardanoWallet <-
          CardanoWalletDao.findById(CardanoWalletFixtures.cardanoWallet1.id).transact(database).map(_.get)
        addresses <- CardanoWalletAddressDao.findBy(cardanoWallet.id).transact(database)
      } yield (cardanoWallet, addresses, processingResult)).runSyncUnsafe(1.minute)

      // then
      result match {
        case scheduled: ProcessingTaskScheduled[MirrorProcessingTaskState] =>
          scheduled.data.json
            .as[WatchCardanoBlockchainAddressesStateData]
            .toOption
            .value
            .lastCheckedBlockId
            .id mustBe blocksCount
        case _ =>
          fail(s"Processing task finished with: $result result, but should finish with: ProcessingTaskScheduled")
      }

      cardanoWallet.lastGeneratedNo mustBe usedAddressesCount + cardanoConfig.addressCount - 1
      cardanoWallet.lastUsedNo.value mustBe usedAddressesCount - 1

      addresses.size mustBe usedAddressesCount + cardanoConfig.addressCount

      val usedAddresses = addresses.filter(_.usedAt.isDefined)
      usedAddresses.size mustBe usedAddressesCount
      usedAddresses.maxBy(_.sequenceNo).sequenceNo mustBe usedAddressesCount - 1

      val notUsedAddresses = addresses.filter(_.usedAt.isEmpty)
      notUsedAddresses.size mustBe cardanoConfig.addressCount
      notUsedAddresses.minBy(_.sequenceNo).sequenceNo mustBe usedAddressesCount
      notUsedAddresses.maxBy(_.sequenceNo).sequenceNo mustBe usedAddressesCount + cardanoConfig.addressCount - 1
    }
  }

  trait WatchCardanoBlockchainAddressesProcessorFixtures extends ProcessingTaskFixtures {
    val processingTaskService = new ProcessingTaskServiceStub[MirrorProcessingTaskState]
    val processor =
      new WatchCardanoBlockchainAddressesProcessor(
        database,
        database,
        cardanoConfig,
        cardanoAddressServiceStub,
        processingTaskService
      )
    val data = WatchCardanoBlockchainAddressesStateData(CardanoBlockId(id = 0))
    val processingTask = createProcessingTask[MirrorProcessingTaskState](
      state = MirrorProcessingTaskState.WatchCardanoBlockchainAddressesState,
      data = ProcessingTaskData(data.asJson)
    )
  }

}
