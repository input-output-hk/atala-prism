package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.models.Wallet
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

  def toWalletModel(data: protos.WalletData): Wallet = {
    val maybe = for {
      protoKeyPair <- data.keyPair
      protoPublicKey <- protoKeyPair.publicKey
      protoPrivateKey <- protoKeyPair.privateKey
      publicKey = toPublicKey(protoPublicKey)
      privateKey = toPrivateKey(protoPrivateKey)
    } yield Wallet(data.did, privateKey, publicKey)

    maybe.headOption.getOrElse(fatalWalletCorrupted)
  }

  def toPrivateKey(proto: protos.ECPrivateKey): PrivateKey = {
    proto.d
      .map(_.value)
      .map(BigInt.apply)
      .map(ECKeys.toPrivateKey)
      .getOrElse(fatalWalletCorrupted)
  }

  def toPublicKey(proto: protos.ECPublicKey): PublicKey = {
    val maybe = for {
      x <- proto.x.map(_.value).map(BigInt.apply)
      y <- proto.y.map(_.value).map(BigInt.apply)
    } yield ECKeys.toPublicKey(x, y)

    maybe.getOrElse(fatalWalletCorrupted)
  }

  private def fatalWalletCorrupted = {
    throw new RuntimeException("The wallet is likely corrupted, you'll need to repair it or delete it manually")
  }

  private def loadWallet(file: os.ReadablePath): protos.WalletData = {
    val data = os.read.bytes(file)
    logger.info("Previous wallet found, loading it")

    val wallet = protos.WalletData.parseFrom(data)
    logger.info("Wallet loaded")

    wallet
  }

  private def generateDid(): String = {
    val id = Option(System.getenv(ID_ENV_VARIABLE)).getOrElse {
      "test-" + (1 to 8).map(_ => ('a' + Random.nextInt(26)).toChar).mkString("")
    }
    "did:iohk:" + id
  }

  private def createNewWallet(file: os.Path): protos.WalletData = {
    logger.info("Generating keys")
    val pair = ECKeys.generateKeyPair()
    val protoKeyPair = protos
      .KeyPair()
      .withPrivateKey(toPrivateKeyProto(pair.getPrivate))
      .withPublicKey(toPublicKeyProto(pair.getPublic))

    val wallet = protos
      .WalletData()
      .withKeyPair(Seq(protoKeyPair))
      .withDid("did:iohk:test")

    logger.info("Storing wallet")
    os.write(file, wallet.toByteArray, createFolders = true)

    wallet
  }

  private def toPublicKeyProto(key: PublicKey): protos.ECPublicKey = {
    val point = ECKeys.getECPoint(key)
    protos
      .ECPublicKey()
      .withX(protos.BigInteger(point.getAffineX.toString))
      .withY(protos.BigInteger(point.getAffineY.toString))
  }

  private def toPrivateKeyProto(key: PrivateKey): protos.ECPrivateKey = {
    protos.ECPrivateKey().withD(protos.BigInteger(ECKeys.getD(key).toString))
  }
}
