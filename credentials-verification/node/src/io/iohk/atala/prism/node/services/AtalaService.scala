package io.iohk.atala.prism.node.services

import java.time.Instant

import enumeratum.{Enum, EnumEntry}
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.models.{Ledger, TransactionInfo}
import io.iohk.atala.prism.node.AtalaReferenceLedger
import io.iohk.atala.prism.node.bitcoin.BitcoinClient
import io.iohk.atala.prism.node.bitcoin.models.{OpData, _}
import io.iohk.atala.prism.node.services.AtalaService.{BitcoinNetwork, Result}
import io.iohk.atala.prism.node.services.models.{AtalaObjectUpdate, ObjectHandler}
import io.iohk.atala.prism.utils.FutureEither
import io.iohk.atala.prism.utils.FutureEither.FutureEitherOps
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait AtalaService extends AtalaReferenceLedger {
  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit]
}

class AtalaServiceImpl(
    network: BitcoinNetwork,
    bitcoinClient: BitcoinClient,
    binaryOps: BinaryOps,
    onNewReference: ObjectHandler
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

  override def publishReference(ref: SHA256Digest): Future[TransactionInfo] = {
    val opDataBytes: Array[Byte] = ATALA_HEADER ++ ref.value

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

  override def publishObject(bytes: Array[Byte]): Future[TransactionInfo] = {
    throw new NotImplementedError("Publishing whole objects not implemented for Bitcoin ledger")
  }

  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit] = {
    logger trace "Starting look for transactions in a block just synchronized"
    bitcoinClient
      .getFullBlock(blockhash)
      .flatMap { block =>
        logger trace "Block just retrieved from the Bitcoin blockchain"
        val atalaReferences: List[(SHA256Digest, TransactionInfo)] = for {
          tx <- block.transactions
          out <- tx.vout
          _ = logger trace "VOut of a Block detected"
          opData <- binaryOps.extractOpReturn(out.scriptPubKey.asm)
          _ = logger trace s"OP_RETURN detected in a transaction: ${out.scriptPubKey.asm}"
          trimmed = binaryOps.trimZeros(opData)
          if trimmed.startsWith(ATALA_HEADER)
          // this is an Atala transaction
          data = trimmed.drop(ATALA_HEADER.length)
          _ = logger info s"New Atala transaction found in the chain: ${binaryOps.convertBytesToHex(data)}"
          atalaObjectId = SHA256Digest(data)
        } yield (atalaObjectId, TransactionInfo(tx.id, ledger))
        logger trace s"Found ${atalaReferences.size} ATALA references"

        Future
          .traverse(atalaReferences) { atalaReference =>
            val (reference, transactionInfo) = atalaReference
            // TODO: Update Instant.ofEpochMilli(block.header.time) for proper expression
            onNewReference(
              AtalaObjectUpdate.Reference(reference),
              Instant.ofEpochMilli(block.header.time),
              transactionInfo
            )
          }
          .map(_ => Right(()))
          .toFutureEither
      }
      .recoverLeft(_ => ())
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
      onNewReference: ObjectHandler
  )(implicit
      ec: ExecutionContext
  ): AtalaService = {
    val binaryOps = BinaryOps()
    new AtalaServiceImpl(network, bitcoinClient, binaryOps, onNewReference)
  }
}
