package io.iohk.atala.mirror.task.lease.system.processors

import cats.data.EitherT
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.mirror.config.CardanoConfig
import io.iohk.atala.mirror.db.{CardanoDBSyncDao, CardanoWalletAddressDao, CardanoWalletDao}
import io.iohk.atala.mirror.models.{
  CardanoAddress,
  CardanoAddressBlockInfo,
  CardanoBlockId,
  CardanoWallet,
  CardanoWalletAddress
}
import io.iohk.atala.mirror.services.CardanoAddressService
import io.iohk.atala.mirror.services.CardanoAddressService.CardanoAddressServiceError
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskResult.MirrorProcessingTaskResult
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskState
import io.iohk.atala.mirror.task.lease.system.data.WatchCardanoBlockchainAddressesStateData
import io.iohk.atala.mirror.task.lease.system.processors.WatchCardanoBlockchainAddressesProcessor.AddressesGenerationResult
import io.iohk.atala.prism.task.lease.system.{
  ProcessingTask,
  ProcessingTaskData,
  ProcessingTaskId,
  ProcessingTaskProcessor,
  ProcessingTaskResult,
  ProcessingTaskService
}
import monix.eval.Task
import org.slf4j.LoggerFactory
import io.iohk.atala.prism.task.lease.system.ProcessingTaskProcessorOps._
import io.circe.syntax._
import io.iohk.atala.mirror.task.lease.system.MirrorProcessingTaskState._

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.annotation.tailrec

class WatchCardanoBlockchainAddressesProcessor(
    tx: Transactor[Task],
    cardanoDbSyncTransactor: Transactor[Task],
    cardanoConfig: CardanoConfig,
    cardanoAddressService: CardanoAddressService,
    processingTaskService: ProcessingTaskService[MirrorProcessingTaskState]
) extends ProcessingTaskProcessor[MirrorProcessingTaskState] {

  private val BLOCKS_BATCH_SIZE_LIMIT = 500

  private implicit val logger = LoggerFactory.getLogger(this.getClass)

  override def process(
      processingTask: ProcessingTask[MirrorProcessingTaskState],
      workerNumber: Int
  ): Task[MirrorProcessingTaskResult] = {
    (for {
      data <-
        parseProcessingTaskData[WatchCardanoBlockchainAddressesStateData, MirrorProcessingTaskState](processingTask)

      lastBlockId <- EitherT(Task.tailRecM(data.lastCheckedBlockId) { blockId =>
        processBlocks(processingTask.id, workerNumber, blockId).map {
          case Right(lastBlockId) if lastBlockId.id == blockId.id + BLOCKS_BATCH_SIZE_LIMIT =>
            Left(lastBlockId) // continue iteration
          case Right(lastBlockId) =>
            Right( // end iteration
              Right(lastBlockId) // with success
            )
          case Left(error) =>
            Right( // end iteration
              Left(error) // with error
            )
        }
      })

      newData = ProcessingTaskData(WatchCardanoBlockchainAddressesStateData(lastBlockId).asJson)

    } yield ProcessingTaskResult.ProcessingTaskScheduled(
      state = WatchCardanoBlockchainAddressesState,
      data = newData,
      scheduledTime = Instant.now().plus(cardanoConfig.syncIntervalInSeconds.toLong, ChronoUnit.SECONDS)
    )).value.map(_.merge)
  }

  private def processBlocks(
      processingTaskId: ProcessingTaskId,
      workerNumber: Int,
      blockId: CardanoBlockId
  ): Task[Either[MirrorProcessingTaskResult, CardanoBlockId]] = {
    println(s"Processing blocks from $blockId")
    (for {
      blockInfo <-
        EitherT.liftF[Task, MirrorProcessingTaskResult, (Option[CardanoBlockId], List[CardanoAddressBlockInfo])](
          CardanoDBSyncDao
            .findAddressesInBlockWithLastBlockId(blockId, BLOCKS_BATCH_SIZE_LIMIT)
            .transact(cardanoDbSyncTransactor)
        )
      (lastBlockIdOption, addressesWithBlockInfo) = blockInfo
      allAddressesMap = addressesWithBlockInfo.map(address => (address.cardanoAddress, address)).toMap
      cardanoAddresses = addressesWithBlockInfo.map(_.cardanoAddress)
      localAddresses <- EitherT.liftF(CardanoWalletAddressDao.findAddresses(cardanoAddresses).transact(tx))
      localAddressesGrouped = localAddresses.groupBy(_.walletId)
      wallets <- EitherT.liftF[Task, MirrorProcessingTaskResult, List[CardanoWallet]](
        CardanoWalletDao.findByIds(localAddressesGrouped.keys.toList).transact(tx)
      )
      _ <- wallets.traverse { wallet =>
        processWalletAddresses(wallet, localAddressesGrouped(wallet.id), allAddressesMap)
      }

      lastBlockId = lastBlockIdOption.getOrElse(blockId)
      newData = ProcessingTaskData(WatchCardanoBlockchainAddressesStateData(lastBlockId).asJson)
      _ <- EitherT.liftF[Task, MirrorProcessingTaskResult, Unit](
        processingTaskService.updateData(processingTaskId, workerNumber, newData)
      )
    } yield lastBlockId).value
  }

  private def processWalletAddresses(
      wallet: CardanoWallet,
      walletAddresses: List[CardanoWalletAddress],
      allAddresses: Map[CardanoAddress, CardanoAddressBlockInfo]
  ): EitherT[Task, MirrorProcessingTaskResult, Unit] = {

    val lastUsedAddressNumber = walletAddresses.maxBy(_.sequenceNo).sequenceNo

    for {
      _ <- EitherT.liftF[Task, MirrorProcessingTaskResult, Unit](
        (walletAddresses
          .traverse { walletAddress =>
            CardanoWalletAddressDao.updateUsedAt(
              walletAddress.address,
              CardanoWalletAddress.UsedAt(allAddresses(walletAddress.address).blockIssueTime)
            )
          } *> CardanoWalletDao.updateLastUsedNo(wallet.id, lastUsedAddressNumber))
          .transact(tx)
          .void
      )

      _ <- ensureEnoughEmptyAddresses(wallet, allAddresses, lastUsedAddressNumber)
    } yield ()
  }

  private def ensureEnoughEmptyAddresses(
      wallet: CardanoWallet,
      allAddresses: Map[CardanoAddress, CardanoAddressBlockInfo],
      lastUsedAddressNumber: Int
  ): EitherT[Task, MirrorProcessingTaskResult, Unit] = {
    val enoughEmptyAddresses = wallet.lastGeneratedNo - lastUsedAddressNumber >= cardanoConfig.addressCount

    if (!enoughEmptyAddresses) {
      for {
        addressGenerationResult <-
          EitherT
            .fromEither[Task](
              generateAddresses(
                wallet = wallet,
                newAddressesAccumulator = List.empty,
                allAddresses = allAddresses,
                index = wallet.lastGeneratedNo + 1
              )
            )
            .leftMap { cardanoAddressServiceError =>
              logger.error(cardanoAddressServiceError.exception.toString)
              cardanoAddressServiceError.exception.printStackTrace()
              ProcessingTaskResult.ProcessingTaskRestart
            }

        _ <- EitherT.liftF[Task, MirrorProcessingTaskResult, Unit](
          updateWalletAndAddresses(wallet.id, addressGenerationResult)
        )
      } yield ()
    } else {
      val result: EitherT[Task, MirrorProcessingTaskResult, Unit] = EitherT.pure(())
      result
    }
  }

  private def updateWalletAndAddresses(
      walletId: CardanoWallet.Id,
      addressesGenerationResult: AddressesGenerationResult
  ): Task[Unit] = {
    (for {
      _ <- CardanoWalletAddressDao.insertMany.updateMany(addressesGenerationResult.addresses)
      _ <- CardanoWalletDao.updateLastGeneratedAndUsedNo(
        walletId,
        addressesGenerationResult.lastUsedNo,
        addressesGenerationResult.lastGeneratedNo
      )
    } yield ()).transact(tx)
  }

  @tailrec
  private def generateAddresses(
      wallet: CardanoWallet,
      newAddressesAccumulator: List[CardanoWalletAddress],
      allAddresses: Map[CardanoAddress, CardanoAddressBlockInfo],
      index: Int
  ): Either[CardanoAddressServiceError, AddressesGenerationResult] = {
    cardanoAddressService.generateWalletAddress(wallet.extendedPublicKey, index, cardanoConfig.network.name) match {
      case Right(address) =>
        allAddresses.get(address) match {
          case Some(usedAddressInfo) =>
            val cardanoWalletAddress = CardanoWalletAddress(
              address,
              wallet.id,
              index,
              Some(CardanoWalletAddress.UsedAt(usedAddressInfo.blockIssueTime))
            )
            generateAddresses(
              wallet,
              cardanoWalletAddress :: newAddressesAccumulator,
              allAddresses,
              index + 1
            )

          case None =>
            val cardanoWalletAddress = CardanoWalletAddress(address, wallet.id, index, None)
            cardanoAddressService
              .generateWalletAddresses(
                wallet.extendedPublicKey,
                index + 1,
                cardanoConfig.addressCount + index,
                cardanoConfig.network.name
              )
              .map { addressesWithSequenceNumbers =>
                val cardanoWalletAddresses = addressesWithSequenceNumbers.map {
                  case (address, number) =>
                    CardanoWalletAddress(address, wallet.id, number, None)
                }
                AddressesGenerationResult(
                  cardanoWalletAddresses ++ (cardanoWalletAddress :: newAddressesAccumulator),
                  lastGeneratedNo = index - 1 + cardanoConfig.addressCount,
                  lastUsedNo = index - 1
                )
              }
        }
      case Left(error) => Left(error)
    }
  }
}

object WatchCardanoBlockchainAddressesProcessor {
  private case class AddressesGenerationResult(
      addresses: List[CardanoWalletAddress],
      lastGeneratedNo: Int,
      lastUsedNo: Int
  )
}
