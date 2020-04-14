package io.iohk.cvp.wallet

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.{ECKeys, SHA256Digest}
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
    val keyPairs = List(protoMasterKeyPair)

    val createOperation = ECKeyOperation.toSignedAtalaOperation(keyPairs)
    val unsigedOperation = createOperation.operation
      .getOrElse(throw new RuntimeException("Impossible, the operation must be present"))
    val didSuffix = SHA256Digest.compute(unsigedOperation.toByteArray)
    val did = s"did:prism:${didSuffix.hexValue}"

    val wallet = wallet_internal
      .WalletData()
      .withKeyPair(keyPairs)
      .withOrganisationName(organisationName)
      .withRole(role)
      .withLogo(ByteString.copyFrom(logo))
      .withDid(did)
      .withCreateDidSignedOperation(createOperation)

    wallet
  }

  private def generateProtoKeyPair(id: String): wallet_internal.KeyPair = {
    val keyPair = ECKeys.generateKeyPair()
    wallet_internal
      .KeyPair()
      .withId(id)
      .withUsage(io.iohk.prism.protos.node_models.KeyUsage.MASTER_KEY)
      .withPrivateKey(WalletHelper.toPrivateKeyProto(keyPair.getPrivate))
      .withPublicKey(WalletHelper.toPublicKeyProto(keyPair.getPublic))
  }
}

object WalletServiceOrchestrator {

  def apply(walletSecurity: WalletSecurity, walletIO: WalletIO)(
      implicit ec: ExecutionContext
  ): WalletServiceOrchestrator =
    new WalletServiceOrchestrator(walletSecurity, walletIO)
}
