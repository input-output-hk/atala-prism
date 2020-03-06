package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.prism.protos.{wallet_internal, wallet_models}
import org.slf4j.LoggerFactory

import scala.util.Random

object WalletHelper {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val ID_ENV_VARIABLE = "ATALA_WALLET_ID"

  def getOrCreate(): Wallet = {
    val walletFile = os.pwd / ".cvpwallet" / "wallet.dat"
    val protoWallet = if (os.exists(walletFile)) {
      loadWallet(walletFile)
    } else {
      createNewWallet(walletFile)
    }

    toWalletModel(protoWallet)
  }

  def toWalletModel(data: wallet_internal.WalletData): Wallet = {
    val maybe = for {
      protoKeyPair <- data.keyPair
      protoPublicKey <- protoKeyPair.publicKey
      protoPrivateKey <- protoKeyPair.privateKey
      publicKey = toPublicKey(protoPublicKey)
      privateKey = toPrivateKey(protoPrivateKey)
    } yield Wallet(data.did, privateKey, publicKey)

    maybe.headOption.getOrElse(fatalWalletCorrupted)
  }

  def toPrivateKey(proto: wallet_models.ECPrivateKey): PrivateKey = {
    proto.d
      .map(_.value)
      .map(BigInt.apply)
      .map(ECKeys.toPrivateKey)
      .getOrElse(fatalWalletCorrupted)
  }

  def toPublicKey(proto: wallet_models.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(fatalWalletCorrupted)
  }

  private def fatalWalletCorrupted = {
    throw new RuntimeException("The wallet is likely corrupted, you'll need to repair it or delete it manually")
  }

  private def loadWallet(file: os.ReadablePath): wallet_internal.WalletData = {
    val data = os.read.bytes(file)
    logger.info("Previous wallet found, loading it")

    val wallet = wallet_internal.WalletData.parseFrom(data)
    logger.info("Wallet loaded")

    wallet
  }

  def generateDid(): String = {
    val id = Option(System.getenv(ID_ENV_VARIABLE)).getOrElse {
      "test-" + Random.alphanumeric.take(8).mkString("")
    }
    "did:iohk:" + id
  }

  private def createNewWallet(file: os.Path): wallet_internal.WalletData = {
    logger.info("Generating keys")
    val pair = ECKeys.generateKeyPair()
    val protoKeyPair = wallet_internal
      .KeyPair()
      .withPrivateKey(toPrivateKeyProto(pair.getPrivate))
      .withPublicKey(toPublicKeyProto(pair.getPublic))

    val wallet = wallet_internal
      .WalletData()
      .withKeyPair(Seq(protoKeyPair))
      .withDid(generateDid())

    logger.info("Storing wallet")
    os.write(file, wallet.toByteArray, createFolders = true)

    wallet
  }

  private def toPublicKeyProto(key: PublicKey): wallet_models.ECPublicKey = {
    val point = ECKeys.getECPoint(key)
    wallet_models
      .ECPublicKey()
      .withX(wallet_models.BigInteger(point.getAffineX.toString))
      .withY(wallet_models.BigInteger(point.getAffineY.toString))
  }

  private def toPrivateKeyProto(key: PrivateKey): wallet_models.ECPrivateKey = {
    wallet_models.ECPrivateKey().withD(wallet_models.BigInteger(ECKeys.getD(key).toString))
  }
}
