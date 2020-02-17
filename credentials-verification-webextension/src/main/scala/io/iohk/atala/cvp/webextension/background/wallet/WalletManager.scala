package io.iohk.atala.cvp.webextension.background.wallet

import java.util.Base64

import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserActionService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.facades.elliptic.{EC, KeyPair}
import org.scalajs.dom
import org.scalajs.dom.crypto
import org.scalajs.dom.crypto.{CryptoKey, KeyFormat}

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

case class WalletData(keys: Map[String, String])

private[background] class WalletManager(browserActionService: BrowserActionService, storageService: StorageService)(
    implicit ectx: ExecutionContext
) {
  val ec: EC = new EC(WalletManager.CURVE_NAME)

  private var storageKey: Option[CryptoKey] = None
  var keys: Map[String, KeyPair] = Map.empty
  var signingRequests: Map[Int, (SigningRequest, Promise[String])] = Map.empty
  var requestCounter: Int = 0

  private def updateBadge(): Unit = {
    val badgeText = if (signingRequests.nonEmpty) signingRequests.size.toString else ""
    browserActionService.setBadgeText(badgeText)
  }

  def createKey(name: String): KeyPair = {
    if (keys.contains(name)) {
      throw new IllegalArgumentException("Key exists")
    } else {
      val key = ec.genKeyPair()
      keys += name -> key
      save()
      key
    }
  }

  def listKeys(): Seq[String] = {
    keys.keys.toSeq
  }

  def requestSignature(message: String): Future[String] = {
    val signaturePromise = Promise[String]()

    requestCounter += 1
    val request = SigningRequest(requestCounter, message)
    signingRequests += requestCounter -> (request, signaturePromise)
    updateBadge()

    signaturePromise.future
  }

  def getSigningRequests(): Seq[SigningRequest] = {
    signingRequests.values.map(_._1).toSeq
  }

  def signWith(requestId: Int, keyName: String): Unit = {
    val (request, promise) = signingRequests.getOrElse(requestId, throw new IllegalArgumentException("Unknown request"))
    val key = keys.getOrElse(keyName, throw new IllegalArgumentException("Unknown key"))

    val signature = key.sign(request.message)

    signingRequests -= requestId
    updateBadge()
    promise.success(signature.toDER("hex"))
  }

  def toWalletData(): WalletData = {
    val serializedKeys = keys.map {
      case (keyName, keyPair) =>
        (keyName, keyPair.getPrivate("hex"))
    }
    WalletData(serializedKeys)
  }

  def loadFromWalletData(data: WalletData): Unit = {
    keys = data.keys.map {
      case (keyName, serializedKey) =>
        (keyName, ec.keyFromPrivate(serializedKey, "hex"))
    }
  }

  def initialAesCtr: crypto.AesCtrParams = {
    val counter = Array.tabulate[Byte](16)(i => if (i == 15) 1.toByte else 0.toByte)
    crypto.AesCtrParams(
      "AES-CTR",
      counter.toTypedArray.buffer,
      32
    )
  }

  def save(): Future[Unit] = {
    val walletData = toWalletData()
    val json = walletData.asJson.noSpaces
    dom.console.log(s"Serialized wallet: $json")

    val result = for {
      arrayBuffer <- crypto.crypto.subtle
        .encrypt(initialAesCtr, storageKey.get, json.getBytes.toTypedArray.buffer)
        .toFuture
        .asInstanceOf[Future[ArrayBuffer]]
      arr = Array.ofDim[Byte](arrayBuffer.byteLength)
      _ = TypedArrayBuffer.wrap(arrayBuffer).get(arr)
      encodedJson = new String(Base64.getEncoder.encode(arr))
      _ = dom.console.log(s"Encrypted wallet: $encodedJson")
      _ = storageService.store(WalletManager.LOCAL_STORAGE_KEY, encodedJson)
    } yield ()

    result.onComplete {
      case Success(value) => dom.console.log("Successfully saved wallet")
      case Failure(exception) => dom.console.log("Failed saving wallet"); exception.printStackTrace()
    }

    result
  }

  def getStatus(): Future[WalletStatus] = {
    for {
      storedData <- storageService.load(WalletManager.LOCAL_STORAGE_KEY)
    } yield {
      if (storedData.isEmpty) {
        WalletStatus.Missing
      } else if (this.storageKey.nonEmpty) {
        WalletStatus.Unlocked
      } else {
        WalletStatus.Locked
      }
    }
  }

  def unlock(password: String): Future[Unit] = {
    val pbdkf2 =
      crypto.Pbkdf2Params("PBKDF2", WalletManager.PASSWORD_SALT.getBytes.toTypedArray.buffer, 100L, "SHA-512")

    val result = for {
      pbkdf2Key <- crypto.crypto.subtle
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
      aesKey <- crypto.crypto.subtle
        .deriveKey(
          pbdkf2,
          pbkdf2Key,
          aesCtr,
          true,
          js.Array(crypto.KeyUsage.encrypt, crypto.KeyUsage.decrypt)
        )
        .toFuture
        .asInstanceOf[Future[crypto.CryptoKey]]
      _ = { storageKey = Some(aesKey) }
      storedEncryptedJson <- storageService.load(WalletManager.LOCAL_STORAGE_KEY)
      json <- {
        dom.console.log(s"Serialized wallet: $storedEncryptedJson")
        if (storedEncryptedJson.isEmpty) {
          Future.successful("{}")
        } else {
          val encryptedJson = storedEncryptedJson.get.asInstanceOf[String]
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
      }
      _ = dom.console.log(s"Loading wallet from: ${json}")
      walletData = parse(json).getOrElse(Json.obj()).as[WalletData].getOrElse(WalletData(Map.empty))
      _ = loadFromWalletData(walletData)
    } yield ()

    result.onComplete {
      case Success(value) => dom.console.log("Successfully loaded wallet")
      case Failure(exception) => dom.console.log("Failed loading wallet"); exception.printStackTrace()
    }

    result
  }

  def lock(): Unit = {
    this.storageKey = None
  }
}
