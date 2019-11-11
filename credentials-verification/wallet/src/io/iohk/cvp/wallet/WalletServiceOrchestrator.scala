package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.protos.WalletData
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

object WalletServiceOrchestrator {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val walletSecurity = WalletSecurity()

  def createNewWallet(passphrase: String)(implicit ec: ExecutionContext): Future[Unit] = {
    for {
      keySpec <- Future { walletSecurity.generateSecretKey(passphrase) }
      wallet = generateWallet()
      encryptedWallet = walletSecurity.encrypt(keySpec, wallet.toByteArray)
      save <- WalletIO().save(encryptedWallet)
    } yield save
  }

  def loadWallet(passphrase: Option[String] = None)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
//    val keySpec: SecretKeySpec = generateSecretKey(
//      passphrase.getOrElse(throw new RuntimeException("Passphrase required"))
//    ) //TODO IN NEXT STORY
    WalletIO().load()
  }

  private def generateWallet(): WalletData = {
    logger.info("Generating keys")
    val pair = ECKeys.generateKeyPair()
    val protoKeyPair = protos
      .KeyPair()
      .withPrivateKey(toPrivateKeyProto(pair.getPrivate))
      .withPublicKey(toPublicKeyProto(pair.getPublic))

    val wallet = protos
      .WalletData()
      .withKeyPair(protoKeyPair)
      .withDid("did:iohk:test")

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
