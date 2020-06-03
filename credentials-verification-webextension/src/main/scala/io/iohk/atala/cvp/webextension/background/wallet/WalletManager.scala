package io.iohk.atala.cvp.webextension.background.wallet

import java.util.Base64
import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserActionService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, Mnemonic}
import io.iohk.atala.cvp.webextension.facades.elliptic.{EC, KeyPair}
import io.iohk.prism.protos.connector_api.{GetCurrentUserRequest, GetCurrentUserResponse, RegisterDIDRequest}
import org.scalajs.dom.crypto
import org.scalajs.dom.crypto.{CryptoKey, KeyFormat}
import scala.scalajs.js.JSConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, _}
import scala.util.{Failure, Success}

object WalletManager {
  val CURVE_NAME = "secp256k1"

  val LOCAL_STORAGE_KEY = "atala-wallet-data"
  val PASSWORD_SALT = "kkmarcbr/a"

  // TODO: remove as soon as proper wallet creation, locking/unlocking is implemented
  val FIXME_WALLET_PASSWORD = "abc"
}

case class SigningRequest(id: Int, message: String)

sealed trait WalletStatus

object WalletStatus {

  final case object Missing extends WalletStatus

  final case object Unlocked extends WalletStatus

  final case object Locked extends WalletStatus

}

sealed trait Role

object Role {

  final case object Issuer extends Role

  final case object Verifier extends Role

  def toConnectorApiRole(role: Role): RegisterDIDRequest.Role = {
    role match {
      case Issuer => RegisterDIDRequest.Role.issuer
      case Verifier => RegisterDIDRequest.Role.verifier
    }
  }

  def toRole(role: GetCurrentUserResponse.Role): Role = {
    role match {
      case GetCurrentUserResponse.Role.issuer => Issuer
      case GetCurrentUserResponse.Role.verifier => Verifier
    }
  }

  def toRole(value: String): Role = {
    value match {
      case "Issuer" => Issuer
      case "Verifier" => Verifier
    }
  }
}

case class WalletData(
    keys: Map[String, String],
    mnemonic: Mnemonic,
    organisationName: String,
    role: Role,
    logo: Array[Byte]
) {
  def addKey(name: String, key: String): WalletData = {
    copy(keys = keys + (name -> key))
  }
}

private[background] class WalletManager(
    browserActionService: BrowserActionService,
    storageService: StorageService,
    connectorClientService: ConnectorClientService
)(implicit
    ectx: ExecutionContext
) {
  val ec: EC = new EC(WalletManager.CURVE_NAME)

  private var storageKey: Option[CryptoKey] = None
  var walletData: Option[WalletData] = None
  var signingRequests: Map[Int, (SigningRequest, Promise[String])] = Map.empty
  var requestCounter: Int = 0

  private def updateBadge(): Unit = {
    val badgeText = if (signingRequests.nonEmpty) signingRequests.size.toString else ""
    browserActionService.setBadgeText(badgeText)
  }

  private def updateStorageKeyAndWalletData(storageKey: CryptoKey, walletData: WalletData): Unit = {
    this.storageKey = Some(storageKey)
    this.walletData = Some(walletData)
  }

  def createKey(name: String): Future[KeyPair] = {
    (walletData, storageKey) match {
      case (Some(wallet), Some(encryptionKey)) =>
        if (wallet.keys.contains(name)) {
          Future.failed(new IllegalArgumentException("Key exists"))
        } else {
          val newKeyPair = ec.genKeyPair()
          val newWalletData = wallet.addKey(name, newKeyPair.getPrivate("hex"))
          for {
            _ <- save(encryptionKey, newWalletData)
            _ = updateStorageKeyAndWalletData(storageKey.get, newWalletData)
          } yield newKeyPair
        }
      case _ => Future.failed(new RuntimeException("The wallet has not been loaded"))
    }
  }

  def listKeys(): Seq[String] = {
    walletData.map(_.keys.keys.toSeq).getOrElse(Seq.empty)
  }

  def requestSignature(message: String): Future[String] = {
    val signaturePromise = Promise[String]()

    requestCounter += 1
    val request = SigningRequest(requestCounter, message)
    signingRequests += requestCounter -> ((request, signaturePromise))
    updateBadge()

    signaturePromise.future
  }

  def getSigningRequests(): Seq[SigningRequest] = {
    signingRequests.values.map(_._1).toSeq
  }

  def signWith(requestId: Int, keyName: String): Unit = {
    val (request, promise) = signingRequests.getOrElse(requestId, throw new IllegalArgumentException("Unknown request"))
    val key = getKey(keyName)

    val signature = key.sign(request.message)

    signingRequests -= requestId
    updateBadge()
    promise.success(signature.toDER("hex"))
  }

  private def getKey(keyName: String): KeyPair = {
    val serializedKey =
      walletData
        .map(_.keys)
        .getOrElse(Map.empty)
        .getOrElse(keyName, throw new RuntimeException(s"Unknown key $keyName"))
    ec.keyFromPrivate(serializedKey, "hex")
  }

  def initialAesCtr: crypto.AesCtrParams = {
    val counter = Array.tabulate[Byte](16)(i => if (i == 15) 1.toByte else 0.toByte)
    crypto.AesCtrParams(
      "AES-CTR",
      counter.toTypedArray.buffer,
      32
    )
  }

  def save(key: CryptoKey, walletData: WalletData): Future[Unit] = {
    val json = walletData.asJson.noSpaces
    println(s"Serialized wallet: $json")

    val result = for {
      arrayBuffer <-
        crypto.crypto.subtle
          .encrypt(initialAesCtr, key, json.getBytes.toTypedArray.buffer)
          .toFuture
          .asInstanceOf[Future[ArrayBuffer]]
      arr = Array.ofDim[Byte](arrayBuffer.byteLength)
      _ = TypedArrayBuffer.wrap(arrayBuffer).get(arr)
      encodedJson = new String(Base64.getEncoder.encode(arr))
      _ = println(s"Encrypted wallet: $encodedJson")
      _ = storageService.store(WalletManager.LOCAL_STORAGE_KEY, encodedJson)
    } yield ()

    result.onComplete {
      case Success(value) => println("Successfully saved wallet")
      case Failure(exception) => println("Failed saving wallet"); exception.printStackTrace()
    }

    result
  }

  def getStatus(): Future[WalletStatus] = {
    // Avoid local storage call when the wallet is already loaded
    if (this.walletData.nonEmpty) {
      Future.successful(WalletStatus.Unlocked)
    } else {
      // Call local storage to figure out if the wallet actually exists
      for {
        storedData <- storageService.load(WalletManager.LOCAL_STORAGE_KEY)
      } yield {
        if (storedData.isEmpty) {
          WalletStatus.Missing
        } else {
          WalletStatus.Locked
        }
      }
    }
  }

  def generateSecretKey(password: String): Future[CryptoKey] = {
    val pbdkf2 =
      crypto.Pbkdf2Params("PBKDF2", WalletManager.PASSWORD_SALT.getBytes.toTypedArray.buffer, 100L, "SHA-512")

    for {
      pbkdf2Key <-
        crypto.crypto.subtle
          .importKey(
            KeyFormat.raw,
            password.getBytes.toTypedArray.buffer,
            "PBKDF2",
            false,
            js.Array(crypto.KeyUsage.deriveKey, crypto.KeyUsage.deriveBits)
          )
          .toFuture
          .asInstanceOf[Future[crypto.CryptoKey]]

      aesCtr = crypto.AesDerivedKeyParams("AES-CTR", 256)
      aesKey <-
        crypto.crypto.subtle
          .deriveKey(
            pbdkf2,
            pbkdf2Key,
            aesCtr,
            true,
            js.Array(crypto.KeyUsage.encrypt, crypto.KeyUsage.decrypt)
          )
          .toFuture
          .asInstanceOf[Future[crypto.CryptoKey]]
    } yield aesKey
  }

  def createWallet(
      password: String,
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[Unit] = {
    val result = for {
      aesKey <- generateSecretKey(password)
      newWalletData = WalletData(Map.empty, mnemonic, organisationName, role, logo)
      _ <- save(aesKey, newWalletData)
      _ = updateStorageKeyAndWalletData(aesKey, newWalletData)
      _ <- registerDid(mnemonic, role, organisationName, logo)
    } yield ()

    result.onComplete {
      case Success(_) => println("Successfully created wallet")
      case Failure(exception) => println("Failed creating wallet"); exception.printStackTrace()
    }

    result
  }

  def recoverWallet(
      password: String,
      mnemonic: Mnemonic
  ): Future[Unit] = {
    val result = for {
      response <- recoverAccount(mnemonic)
      aesKey <- generateSecretKey(password)
      newWalletData = WalletData(Map.empty, mnemonic, response.name, Role.toRole(response.role), response.logo.bytes)
      _ <- save(aesKey, newWalletData)
      _ = updateStorageKeyAndWalletData(aesKey, newWalletData)
    } yield ()

    result.onComplete {
      case Success(_) => println("Successfully recovered wallet")
      case Failure(exception) => println("Failed recovering wallet"); exception.printStackTrace()
    }
    result
  }

  def unlock(password: String): Future[Unit] = {
    val result = for {
      aesKey <- generateSecretKey(password)
      storedEncryptedJsonOption <- storageService.load(WalletManager.LOCAL_STORAGE_KEY)
      json <-
        storedEncryptedJsonOption
          .map { storedEncryptedJson =>
            val encryptedJson = storedEncryptedJson.asInstanceOf[String]
            val encryptedBytes = Base64.getDecoder.decode(encryptedJson.asInstanceOf[String]).toTypedArray.buffer
            crypto.crypto.subtle
              .decrypt(initialAesCtr, aesKey, encryptedBytes)
              .toFuture
              .asInstanceOf[Future[ArrayBuffer]]
              .map { buffer =>
                val arr = Array.ofDim[Byte](buffer.byteLength)
                TypedArrayBuffer.wrap(buffer).get(arr)
                new String(arr, "UTF-8")
              }
          }
          .getOrElse(throw new RuntimeException("You need to create the wallet before unlocking it"))
      _ = updateStorageKeyAndWalletData(aesKey, parseWalletDataFromJson(json))
    } yield ()

    result.onComplete {
      case Success(value) => println("Successfully loaded wallet")
      case Failure(exception) => println("Failed loading wallet"); exception.printStackTrace()
    }

    result
  }

  private def parseWalletDataFromJson(json: String): WalletData = {
    println(s"Parsing wallet data from: ${json}")
    parse(json)
      .getOrElse(Json.obj())
      .as[WalletData]
      .getOrElse(throw new RuntimeException("Wallet could not be loaded from JSON"))
  }

  def lock(): Unit = {
    this.storageKey = None
    this.walletData = None
  }

  private def registerDid(mnemonic: Mnemonic, role: Role, organisationName: String, logo: Array[Byte]): Future[Unit] = {
    val connectRole = Role.toConnectorApiRole(role)
    val logoByteString = ByteString.copyFrom(logo)
    val registerDIDRequest =
      RegisterDIDRequest(
        Some(ECKeyOperation.toSignedAtalaOperation(mnemonic)),
        connectRole,
        organisationName,
        logoByteString
      )
    connectorClientService.registerDID(registerDIDRequest).map { response =>
      println(s"*****RegisteredDID******=${response.did}")
      ()
    }
  }

  private def recoverAccount(mnemonic: Mnemonic): Future[GetCurrentUserResponse] = {
    val recoverWallet = RecoverWallet(mnemonic)
    val requestNonce = recoverWallet.requestNonce()
    val request = GetCurrentUserRequest()
    val did = "did" -> recoverWallet.createDIDId()
    val didKeyId = "didKeyId" -> ECKeyOperation.firstMasterKeyId
    val didSignature = "didSignature" -> recoverWallet.getUrlEncodedDIDSignature(requestNonce, request)
    val requestNoncePair = "requestNonce" -> recoverWallet.getUrlEncodedRequestNonce(requestNonce)
    val metadata = Map(did, didKeyId, didSignature, requestNoncePair)
    connectorClientService.getCurrentUser(request, metadata.toJSDictionary)
  }
}
