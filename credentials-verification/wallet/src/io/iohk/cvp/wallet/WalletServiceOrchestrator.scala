package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import cats.effect.{ContextShift, IO}
import cats.effect.concurrent.Ref
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.models.Wallet
import io.iohk.cvp.wallet.protos.GetWalletStatusResponse.WalletStatus
import io.iohk.cvp.wallet.protos.WalletData
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

object WalletServiceOrchestrator {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val walletSecurity = WalletSecurity()

  def createNewWallet(passphrase: String)(implicit ec: ExecutionContext): Future[WalletData] = {
    for {
      keySpec <- Future { walletSecurity.generateSecretKey(passphrase) }
      wallet = generateWallet()
      encryptedWallet = walletSecurity.encrypt(keySpec, wallet.toByteArray)
      save <- WalletIO().save(encryptedWallet)
    } yield wallet
  }

  def loadWallet(passphrase: String)(implicit ec: ExecutionContext): Future[Option[Array[Byte]]] = {
    WalletIO().load().map { dataO =>
      dataO.map { data =>
        val keySpec = walletSecurity.generateSecretKey(passphrase)
        walletSecurity.decrypt(keySpec, data)
      }
    }
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
