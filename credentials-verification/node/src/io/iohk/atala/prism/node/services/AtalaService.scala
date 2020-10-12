package io.iohk.atala.prism.node.services

import java.time.Instant

import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.models.{BlockInfo, Ledger, TransactionInfo}
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.bitcoin.BitcoinClient
import io.iohk.atala.prism.node.bitcoin.models.{OpData, _}
import io.iohk.atala.prism.node.services.AtalaService.{BitcoinNetwork, Result}
import io.iohk.atala.prism.node.services.models.{AtalaObjectNotification, AtalaObjectNotificationHandler}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import io.iohk.prism.protos.node_internal
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait AtalaService extends AtalaReferenceLedger {
  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit]
}

class AtalaServiceImpl(
    network: BitcoinNetwork,
    bitcoinClient: BitcoinClient,
    binaryOps: BinaryOps,
    onAtalaObject: AtalaObjectNotificationHandler
)(implicit
    ec: ExecutionContext
) extends AtalaService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val ATALA_HEADER = "ATALA://".getBytes("UTF-8")

  private val ledger: Ledger = {
    if (network == BitcoinNetwork.Testnet) {
      Ledger.BitcoinTestnet
    } else {
      Ledger.BitcoinMainnet
    }
  }

  override def supportsOnChainData: Boolean = false

  override def publish(obj: node_internal.AtalaObject): Future[TransactionInfo] = {
    if (obj.block.isBlockContent) {
      throw new NotImplementedError("Publishing whole objects is not implemented for Bitcoin ledger")
    } else {
      publish(obj.toByteArray)
    }
  }

  private def publish(ref: Array[Byte]): Future[TransactionInfo] = {
    val opDataBytes = ATALA_HEADER ++ ref

    OpData(opDataBytes) match {
      case Some(opData) =>
        bitcoinClient
          .sendDataTx(opData)
          .value
          .map {
            case Right(transactionId) => TransactionInfo(transactionId, ledger)
            case Left(error) =>
              logger.error(s"FATAL: Error while publishing reference: ${error}")
              throw new RuntimeException(s"FATAL: Error while publishing reference: ${error}")
          }
      case None =>
        logger.error(s"FATAL: Atala identifier is too long to store in bitcoin (${opDataBytes.length}")
        Future.failed(
          new RuntimeException(s"FATAL: Atala identifier is too long to store in bitcoin (${opDataBytes.length}")
        )
    }
  }

  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit] = {
    logger trace "Starting look for transactions in a block just synchronized"
    bitcoinClient
      .getFullBlock(blockhash)
      .flatMap { block =>
        logger trace "Block just retrieved from the Bitcoin blockchain"
        // TODO: Update Instant.ofEpochMilli(block.header.time) for proper expression
        val blockTimestamp = Instant.ofEpochMilli(block.header.time)

        val notifications: List[AtalaObjectNotification] = for {
          (tx, blockIndex) <- block.transactions.zipWithIndex
          out <- tx.vout
          _ = logger trace "VOut of a Block detected"
          opData <- binaryOps.extractOpReturn(out.scriptPubKey.asm)
          _ = logger trace s"OP_RETURN detected in a transaction: ${out.scriptPubKey.asm}"
          trimmed = binaryOps.trimZeros(opData)
          if trimmed.startsWith(ATALA_HEADER)
          // this is an Atala transaction
          data = trimmed.drop(ATALA_HEADER.length)
          atalaObject <- parseAtalaObject(data)
          _ = logger info s"New Atala transaction found in the chain: ${binaryOps.convertBytesToHex(data)}"
        } yield AtalaObjectNotification(
          atalaObject,
          TransactionInfo(
            transactionId = tx.id,
            ledger = ledger,
            block = Some(BlockInfo(number = block.header.height, timestamp = blockTimestamp, index = blockIndex))
          )
        )
        logger trace s"Found ${notifications.size} ATALA references"

        Future
          .traverse(notifications) { onAtalaObject(_) }
          .map(_ => Right(()))
          .toFutureEither
      }
      .recoverLeft(_ => ())
  }

  private def parseAtalaObject(data: Array[Byte]): Option[node_internal.AtalaObject] = {
    val validateAtalaObject = node_internal.AtalaObject.validate(data)
    if (validateAtalaObject.isFailure) {
      logger.warn(s"Could not parse Atala transaction: ${binaryOps.convertBytesToHex(data)}")
    }
    validateAtalaObject.toOption
  }
}

object AtalaService {
  type Result[E, A] = FutureEither[E, A]

  // Given that currently, `storage.put` doesn't return any error
  // we can safely equal `PublishError` to `SendDataTxError`
  type PublishError = SendDataTxError

  sealed trait BitcoinNetwork extends EnumEntry
  object BitcoinNetwork extends Enum[BitcoinNetwork] {
    val values = findValues

    case object Testnet extends BitcoinNetwork
    case object Mainnet extends BitcoinNetwork
  }

  def apply(
      network: BitcoinNetwork,
      bitcoinClient: BitcoinClient,
      onAtalaObject: AtalaObjectNotificationHandler
  )(implicit
      ec: ExecutionContext
  ): AtalaService = {
    val binaryOps = BinaryOps()
    new AtalaServiceImpl(network, bitcoinClient, binaryOps, onAtalaObject)
  }
}
