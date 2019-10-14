package io.iohk.node.services

import io.iohk.cvp.utils.FutureEither
import AtalaService.Result
import AtalaService.PublishError
import io.iohk.node.bitcoin.BitcoinClient
import io.iohk.node.bitcoin.models.OpData
import io.iohk.node.objects.ObjectStorageService
import scala.concurrent.ExecutionContext
import io.iohk.node.atala_bitcoin._
import io.iohk.node.bitcoin.models.SendDataTxError

trait AtalaService {
  def publishAtalaTransaction(tx: AtalaTx): Result[PublishError, Unit]
}

class AtalaServiceImpl(bitcoinClient: BitcoinClient, storage: ObjectStorageService, binaryOps: BinaryOps)(
    implicit ec: ExecutionContext
) {

  def publishAtalaTransaction(tx: AtalaTx): Result[PublishError, Unit] = {
    val block = AtalaBlock("1.0", List(tx))
    val blockBytes = binaryOps.toBytes(block)
    val blockHash = binaryOps.hashHex(block)
    val obj = AtalaObject(blockHash)
    val objBytes = binaryOps.toBytes(obj)
    val objHash = binaryOps.hashHex(obj)
    val opDataString = s"ATALA://$objHash"

    OpData(opDataString) match {
      case Some(opData) =>
        storage.put(blockHash, blockBytes)
        storage.put(objHash, objBytes)
        bitcoinClient
          .sendDataTx(opData)
          .map(_ => ())
      case None =>
        throw new RuntimeException("FATAL: Atala identifier is to long to store in bitcoin")
    }
  }

}

object AtalaService {
  type Result[E, A] = FutureEither[E, A]

  // Given that currently, `storage.put` doesn't return any error
  // we can safely equal `PublishError` to `SendDataTxError`
  type PublishError = SendDataTxError
}
