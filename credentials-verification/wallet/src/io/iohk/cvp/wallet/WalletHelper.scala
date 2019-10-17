package io.iohk.cvp.wallet

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.models.Wallet
import org.slf4j.LoggerFactory

object WalletHelper {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def getOrCreate(): Wallet = {
    val walletFile = os.pwd / ".cvpwallet" / "wallet.dat"
    val protoWallet = if (os.exists(walletFile)) {
      loadWallet(walletFile)
    } else {
      createNewWallet(walletFile)
    }

    toWalletModel(protoWallet)
  }

  def toWalletModel(data: protos.WalletData): Wallet = {
    Wallet(data.did)
  }

  private def loadWallet(file: os.ReadablePath): protos.WalletData = {
    val data = os.read.bytes(file)
    logger.info("Previous wallet found, loading it")

    val wallet = protos.WalletData.parseFrom(data)
    logger.info("Wallet loaded")

    wallet
  }

  private def createNewWallet(file: os.Path): protos.WalletData = {
    logger.info("Generating keys")
    val pair = ECKeys.generateKeyPair()
    val protoKeyPair = protos
      .KeyPair()
      .withPrivateKey(ByteString.copyFrom(pair.getPrivate.getEncoded))
      .withPublicKey(ByteString.copyFrom(pair.getPublic.getEncoded))

    val wallet = protos
      .WalletData()
      .withKeyPair(protoKeyPair)
      .withDid("did:iohk:test")

    logger.info("Storing wallet")
    os.write(file, wallet.toByteArray, createFolders = true)

    wallet
  }
}
