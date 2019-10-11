package io.iohk.cvp.wallet

import io.iohk.cvp.crypto.ECKeys
import org.slf4j.LoggerFactory

object WalletApp {

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val walletFile = os.pwd / ".cvpwallet" / "wallet.dat"
    if (os.exists(walletFile)) {
      loadWallet(walletFile)
    } else {
      createNewWallet(walletFile)
    }
  }

  private def loadWallet(file: os.ReadablePath) = {
    val data = os.read.bytes(file)
    logger.info("Previous wallet found, loading it")

    val wallet = protos.WalletData.parseFrom(data)
    logger.info("Wallet loaded")

    wallet
  }

  private def createNewWallet(file: os.Path) = {
    import com.google.protobuf.ByteString

    logger.info("Generating keys")
    val pair = ECKeys.generateKeyPair()
    val protoKeyPair = protos
      .KeyPair()
      .withPrivateKey(ByteString.copyFrom(pair.getPrivate.getEncoded))
      .withPublicKey(ByteString.copyFrom(pair.getPublic.getEncoded))
    val wallet = protos.WalletData().withKeyPair(protoKeyPair)

    logger.info("Storing wallet")
    os.write(file, wallet.toByteArray, createFolders = true)

    wallet
  }
}
