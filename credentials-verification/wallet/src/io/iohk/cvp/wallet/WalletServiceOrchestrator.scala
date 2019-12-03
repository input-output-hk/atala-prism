package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.wallet.protos.{Role, WalletData}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceOrchestrator(walletSecurity: WalletSecurity, walletIO: WalletIO)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def createNewWallet(passphrase: String, role: Role, organisationName: String): Future[WalletData] = {
    Future {
      if (walletIO.fileExist()) {
        logger.info("Previous wallet found")
        throw new RuntimeException("Previous wallet found")
      } else {
        val wallet = generateWallet(role, organisationName)
        logger.info("Storing wallet")
        save(passphrase, wallet).map(_ => wallet)
      }
    }.flatten
  }

  def loadWallet(passphrase: String): Future[Option[Array[Byte]]] = {
    walletIO.load().map { dataO =>
      dataO.map { data =>
        val keySpec = walletSecurity.generateSecretKey(passphrase)
        walletSecurity.decrypt(keySpec, data)
      }
    }
  }

  def save(passphrase: String, walletData: WalletData): Future[Unit] = {
    for {
      keySpec <- Future { walletSecurity.generateSecretKey(passphrase) }
      encryptedWallet = walletSecurity.encrypt(keySpec, walletData.toByteArray)
      _ <- walletIO.save(encryptedWallet)
    } yield ()
  }

  private def generateWallet(role: Role, organisationName: String): WalletData = {
    logger.info("Generating keys")

    val protoMasterKeyPair = generateProtoKeyPair("master")
    val protoIssuerKeyPair = generateProtoKeyPair("issuing")

    val wallet = protos
      .WalletData()
      .withKeyPair(Seq(protoMasterKeyPair, protoIssuerKeyPair))
      .withOrganisationName(organisationName)
      .withRole(role)

    wallet
  }

  private def generateProtoKeyPair(id: String): protos.KeyPair = {
    val keyPair = ECKeys.generateKeyPair()
    protos
      .KeyPair()
      .withId(id)
      .withPrivateKey(toPrivateKeyProto(keyPair.getPrivate))
      .withPublicKey(toPublicKeyProto(keyPair.getPublic))
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

object WalletServiceOrchestrator {

  def apply(walletSecurity: WalletSecurity, walletIO: WalletIO)(
      implicit ec: ExecutionContext
  ): WalletServiceOrchestrator =
    new WalletServiceOrchestrator(walletSecurity, walletIO)
}
