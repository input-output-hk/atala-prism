package io.iohk.atala.prism.wallet

import org.slf4j.LoggerFactory
import os.Path
import scala.concurrent.{ExecutionContext, Future}

class WalletIO(walletFile: Path)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def fileExist() = os.exists(walletFile)

  def save(byteArray: Array[Byte]): Future[Unit] = {
    Future {
      logger.info("save wallet")
      os.write.over(walletFile, byteArray, createFolders = true)
      ()
    }
  }

  def load(): Future[Option[Array[Byte]]] = {
    Future {
      if (fileExist()) {
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

  private val defaultFile = os.pwd / ".cvpwallet" / "wallet-new.dat"

  def apply()(implicit ec: ExecutionContext): WalletIO = {
    new WalletIO(defaultFile)
  }
  def apply(walletFile: Path)(implicit ec: ExecutionContext): WalletIO = {
    new WalletIO(walletFile)
  }
}
