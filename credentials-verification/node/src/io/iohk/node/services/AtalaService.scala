package io.iohk.node.services

import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.cvp.utils.FutureEither
import io.iohk.cvp.utils.FutureEither.FutureEitherOps
import io.iohk.node.AtalaReferenceLedger
import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.{OpData, _}
import io.iohk.node.services.AtalaService.Result
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

trait AtalaService extends AtalaReferenceLedger {
  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit]
}

class AtalaServiceImpl(
    bitcoinClient: BitcoinClient,
    binaryOps: BinaryOps,
    onNewReference: SHA256Digest => Future[Unit]
)(
    implicit ec: ExecutionContext
) extends AtalaService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val ATALA_HEADER = "ATALA://".getBytes("UTF-8")

  override def publishReference(ref: SHA256Digest): Future[Unit] = {
    val opDataBytes: Array[Byte] = ATALA_HEADER ++ ref.value

    OpData(opDataBytes) match {
      case Some(opData) =>
        bitcoinClient
          .sendDataTx(opData)
          .map(_ => ())
          .value
          .map(_.right.get) // TODO proper error support
      case None =>
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
        val atalaReferences = block.transactions.zipWithIndex.flatMap {
          case (tx, zeroBasedIndex) =>
            val txIndex = zeroBasedIndex + 1
            tx.vout.flatMap { out =>
              logger trace "VOut of a Block detected"
              binaryOps
                .extractOpReturn(out.scriptPubKey.asm)
                .flatMap { opData =>
                  logger trace s"OP_RETURN detected in a transaction: ${out.scriptPubKey.asm}"
                  val trimmed = binaryOps.trimZeros(opData)
                  if (trimmed.startsWith(ATALA_HEADER)) {
                    // this is an Atala transaction
                    val data = trimmed.drop(ATALA_HEADER.length)
                    logger info s"New Atala transaction found in the chain: ${binaryOps.convertBytesToHex(data)}"
                    val atalaObjectId = SHA256Digest(data)
                    Some(atalaObjectId)
                  } else {
                    None
                  }
                }
            }
        }
        logger trace s"Found ${atalaReferences.size} ATALA references"

        Future
          .traverse(atalaReferences) { reference =>
            onNewReference(reference)
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

  def apply(
      bitcoinClient: BitcoinClient,
      onNewReference: SHA256Digest => Future[Unit]
  )(
      implicit ec: ExecutionContext
  ): AtalaService = {
    val binaryOps = BinaryOps()
    new AtalaServiceImpl(bitcoinClient, binaryOps, onNewReference)
  }
}
