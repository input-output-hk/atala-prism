package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import AtalaService.Result
import AtalaService.PublishError
import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.OpData
import io.iohk.node.objects.ObjectStorageService
import scala.concurrent.ExecutionContext
import io.iohk.node.atala_bitcoin._
import io.iohk.node.bitcoin.models._
import org.slf4j.LoggerFactory

trait AtalaService {
  def publishAtalaTransaction(tx: AtalaTx): Result[PublishError, Unit]
  def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit]
}

class AtalaServiceImpl(bitcoinClient: BitcoinClient, storage: ObjectStorageService, binaryOps: BinaryOps)(
    implicit ec: ExecutionContext
) extends AtalaService {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val ATALA_HEADER = "ATALA://".getBytes("UTF-8")

  override def publishAtalaTransaction(tx: AtalaTx): Result[PublishError, Unit] = {
    val block = AtalaBlock("1.0", List(tx))
    val blockBytes = binaryOps.toBytes(block)
    val blockHash = binaryOps.hashHex(block)
    val obj = AtalaObject(blockHash)
    val objBytes = binaryOps.toBytes(obj)
    val objHashBytes: Array[Byte] = binaryOps.hash(obj)
    val objHash: String = binaryOps.hashHex(obj)
    val header: Array[Byte] = ATALA_HEADER
    val opDataBytes: Array[Byte] = header ++ objHashBytes

    OpData(opDataBytes) match {
      case Some(opData) =>
        storage.put(blockHash, blockBytes)
        storage.put(objHash, objBytes)
        bitcoinClient
          .sendDataTx(opData)
          .map(_ => ())
      case None =>
        throw new RuntimeException(s"FATAL: Atala identifier is to long to store in bitcoin (${opDataBytes.length}")
    }
  }

  override def synchronizeBlock(blockhash: Blockhash): Result[Nothing, Unit] = {
    logger trace "Starting look for transacctions in a block just synchronized"
    bitcoinClient
      .getFullBlock(blockhash)
      .map { block =>
        logger trace "Block just retrieved from the Bitcoin blockchain"
        block.transactions.foreach { tx =>
          tx.vout.foreach { out =>
            logger trace "VOut of a Block detected"
            binaryOps
              .extractOpReturn(out.scriptPubKey.asm)
              .foreach { opData =>
                logger trace s"OP_RETURN detected in a transaction: ${out.scriptPubKey.asm}"
                val trimmed = binaryOps.trimZeros(opData)
                if (trimmed.startsWith(ATALA_HEADER)) {
                  // this is an Atala transaction
                  val data = trimmed.drop(ATALA_HEADER.length)
                  logger info s"New Atala transaction found in the chain: ${binaryOps.convertBytesToHex(data)}"
                }
              }
          }
        }
      }
      .recoverLeft(_ => ())
  }
}

object AtalaService {
  type Result[E, A] = FutureEither[E, A]

  // Given that currently, `storage.put` doesn't return any error
  // we can safely equal `PublishError` to `SendDataTxError`
  type PublishError = SendDataTxError

  def apply(bitcoinClient: BitcoinClient, storage: ObjectStorageService)(
      implicit ec: ExecutionContext
  ): AtalaService = {
    val binaryOps = BinaryOps()
    new AtalaServiceImpl(bitcoinClient, storage, binaryOps)
  }
}
