package io.iohk.cvp.wallet

import org.slf4j.LoggerFactory
import os.Path
import scala.concurrent.{ExecutionContext, Future}

class WalletIO(walletFile: Path)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def save(byteArray: Array[Byte]): Future[Unit] = {
    Future {
      if (os.exists(walletFile)) {
        logger.info("Previous wallet found")
        throw new RuntimeException("Previous wallet found")
      } else {
        logger.info("Storing wallet")
        os.write(walletFile, byteArray, createFolders = true)
        ()
      }
    }
  }

  def load(): Future[Option[Array[Byte]]] = {
    Future {
      if (os.exists(walletFile)) {
        logger.info("Previous wallet found, loading it")
        val data = os.read.bytes(walletFile)
        Some(data)
      } else {
        logger.info("wallet not found")
        None
      }
    }
  }
}

object WalletIO {

  private val defaultFile = os.pwd / ".cvpwallet" / "wallet.dat"

  def apply()(implicit ec: ExecutionContext): WalletIO = {
    new WalletIO(defaultFile)
  }
  def apply(walletFile: Path)(implicit ec: ExecutionContext): WalletIO = {
    new WalletIO(walletFile)
  }
}
