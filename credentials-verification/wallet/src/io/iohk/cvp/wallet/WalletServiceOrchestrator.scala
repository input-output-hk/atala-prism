package io.iohk.cvp.wallet

import java.security.{PrivateKey, PublicKey}

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.prism.protos.{wallet_internal, wallet_models}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class WalletServiceOrchestrator(walletSecurity: WalletSecurity, walletIO: WalletIO)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def createNewWallet(
      passphrase: String,
      role: wallet_models.Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[wallet_internal.WalletData] = {
    Future {
      if (walletIO.fileExist()) {
        logger.info("Previous wallet found")
        throw new RuntimeException("Previous wallet found")
      } else {
        val wallet = generateWallet(role, organisationName, logo)
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

  def save(passphrase: String, walletData: wallet_internal.WalletData): Future[Unit] = {
    for {
      keySpec <- Future { walletSecurity.generateSecretKey(passphrase) }
      encryptedWallet = walletSecurity.encrypt(keySpec, walletData.toByteArray)
      _ <- walletIO.save(encryptedWallet)
    } yield ()
  }

  private def generateWallet(
      role: wallet_models.Role,
      organisationName: String,
      logo: Array[Byte]
  ): wallet_internal.WalletData = {
    logger.info("Generating keys")

    val protoMasterKeyPair = generateProtoKeyPair("master")
    val protoIssuerKeyPair = generateProtoKeyPair("issuing")

    val wallet = wallet_internal
      .WalletData()
      .withKeyPair(Seq(protoMasterKeyPair, protoIssuerKeyPair))
      .withOrganisationName(organisationName)
      .withRole(role)
      .withLogo(ByteString.copyFrom(logo))

    wallet
  }

  private def generateProtoKeyPair(id: String): wallet_internal.KeyPair = {
    val keyPair = ECKeys.generateKeyPair()
    wallet_internal
      .KeyPair()
      .withId(id)
      .withPrivateKey(toPrivateKeyProto(keyPair.getPrivate))
      .withPublicKey(toPublicKeyProto(keyPair.getPublic))
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

object WalletServiceOrchestrator {

  def apply(walletSecurity: WalletSecurity, walletIO: WalletIO)(
      implicit ec: ExecutionContext
  ): WalletServiceOrchestrator =
    new WalletServiceOrchestrator(walletSecurity, walletIO)
}
