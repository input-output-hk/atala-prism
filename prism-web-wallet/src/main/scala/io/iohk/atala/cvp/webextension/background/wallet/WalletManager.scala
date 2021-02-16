package io.iohk.atala.cvp.webextension.background.wallet

import java.util.{Base64, UUID}

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.CredentialsCopyJob
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserActionService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.background.services.node.NodeClientService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.background.wallet.WalletManager.{
  AES_KEY_LENGTH,
  AES_MODE,
  GCM_IV_LENGTH,
  TAG_LENGTH_BIT
}
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.ECKeyOperation.{didFromMasterKey, ecKeyPairFromSeed, _}
import io.iohk.atala.cvp.webextension.common.models.Role.{Issuer, Verifier}
import io.iohk.atala.cvp.webextension.common.models._
import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, Mnemonic}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.{GetCurrentUserResponse, RegisterDIDRequest, RegisterDIDResponse}
import org.scalajs.dom.crypto
import org.scalajs.dom.crypto.{CryptoKey, KeyFormat}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, _}
import scala.util.{Failure, Success, Try}

object WalletManager {
  val CURVE_NAME = "secp256k1"

  val LOCAL_STORAGE_KEY = "atala-wallet-data"
  val PASSWORD_SALT = "kkmarcbr/a"
  val AES_MODE = "AES-GCM"
  val AES_KEY_LENGTH: Short = 256
  // GCM MODE
  val GCM_IV_LENGTH = 12 // Initialization Vector
  val TAG_LENGTH_BIT: Short = 128 // Authentication Tag
  // GCM MODE
}

case class SigningRequest(id: Int, origin: String, sessionId: String, subject: CredentialSubject)

sealed trait WalletStatus

object WalletStatus {

  final case object Missing extends WalletStatus

  final case object Unlocked extends WalletStatus

  final case object Locked extends WalletStatus

}

object Role {
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
      case GetCurrentUserResponse.Role.Unrecognized(roleValue) =>
        throw new IllegalArgumentException(s"Unrecognized role $roleValue")
    }
  }
}

case class WalletData(
    keys: Map[String, String],
    mnemonic: Mnemonic,
    organisationName: String,
    did: DID,
    transactionId: Option[String] = None,
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
    connectorClientService: ConnectorClientService,
    nodeClientService: NodeClientService,
    credentialsCopyJob: CredentialsCopyJob
)(implicit
    ec: ExecutionContext
) {
  private val requestAuthenticator = new RequestAuthenticator(EC)

  type SessionID = String
  type Origin = String
  private[this] var session = Set[(SessionID, Origin)]()
  var walletData: Option[WalletData] = None
  var signingRequests: Map[Int, (SigningRequest, Promise[String])] = Map.empty
  var requestCounter: Int = 0

  private def updateBadge(): Unit = {
    val badgeText = if (signingRequests.nonEmpty) signingRequests.size.toString else ""
    browserActionService.setBadgeText(badgeText)
  }

  private def reloadPopup(): Unit = {
    browserActionService.reloadPopup()
  }

  private def updateWalletData(walletData: WalletData): Unit = {
    this.walletData = Some(walletData)
  }

  def getSigningRequests(): Seq[SigningRequest] = {
    signingRequests.values.map(_._1).toSeq
  }

  def signRequestAndPublish(requestId: Int): Future[Unit] = {

    val signingRequestsF = Future.fromTry {
      Try {
        signingRequests.getOrElse(requestId, throw new IllegalArgumentException("Unknown request"))
      }
    }

    val walletDataF = Future.fromTry {
      Try {
        walletData.getOrElse(
          throw new RuntimeException("You need to create the wallet before logging in and creating session")
        )
      }
    }

    for {
      (request, _) <- signingRequestsF
      walletData <- walletDataF
      ecKeyPair = ecKeyPairFromSeed(walletData.mnemonic)
      claims = request.subject.properties.asJson.noSpaces
      signingKeyId = ECKeyOperation.firstMasterKeyId // TODO: this key id should eventually be selected by the user
      //       this should be done when we complete the key derivation flow
      _ <- connectorClientService.signAndPublishBatch(
        ecKeyPair,
        walletData.did,
        signingKeyId,
        List(CredentialData(ConsoleCredentialId(request.subject.id), claims))
      )
    } yield {
      signingRequests -= requestId
      println(s"Signed and Published = ${request.subject.id}")
      updateBadge()
    }
  }

  def rejectRequest(requestId: Int): Future[Unit] = {
    val signingRequestsF = Future.fromTry {
      Try {
        signingRequests.getOrElse(requestId, throw new IllegalArgumentException("Unknown request"))
      }
    }
    signingRequestsF.map {
      case (request, _) =>
        signingRequests -= requestId
        println(s"Rejected = ${request.subject.id}")
        updateBadge()
      case _ => throw new IllegalArgumentException("Unknown request")
    }
  }

  def initialAesGcm: crypto.AesGcmParams = {
    val ivBytes = Array.ofDim[Byte](GCM_IV_LENGTH)
    crypto.crypto.getRandomValues(ivBytes.toTypedArray)
    //Additional data is quite interesting it is data that can be authenticated data that won't be encrypted
    //but will be part of integrity, Hence I was thinking if we can.
    //We can use as origin so we know the request for encryption/decryption came from the same origin
    val additionalData = ""
    crypto.AesGcmParams(
      name = AES_MODE,
      iv = ivBytes.toTypedArray.buffer,
      additionalData = additionalData.getBytes.toTypedArray.buffer,
      tagLength = TAG_LENGTH_BIT
    )
  }

  def save(key: CryptoKey, walletData: WalletData): Future[Unit] = {
    val json = walletData.asJson.noSpaces
    println(s"Serialized wallet: $json")
    val result = for {
      arrayBuffer <-
        crypto.crypto.subtle
          .encrypt(initialAesGcm, key, json.getBytes.toTypedArray.buffer)
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

  def getTransactionId(): Future[String] = {
    Future.fromTry {
      Try {
        val wallet = walletData.getOrElse(
          throw new RuntimeException("You need to create the wallet before requesting did")
        )
        wallet.transactionId.getOrElse(throw new RuntimeException("Transaction Id not found"))
      }
    }
  }

  def getLoggedInUserSession(origin: Origin): Future[UserDetails] = {
    Future.fromTry {
      Try {
        val wallet = walletData.getOrElse(
          throw new RuntimeException("You need to create the wallet before logging in and creating session")
        )

        // We need to reuse an existing session to avoid breaking the websites while using many tabs
        // otherwise, each tab would keep it's own session, breaking the others.
        val sessionId = session
          .find(_._2 == origin)
          .map(_._1)
          .getOrElse { UUID.randomUUID().toString }

        session += (sessionId -> origin)
        UserDetails(
          sessionId,
          wallet.organisationName,
          wallet.role.toString,
          wallet.logo
        )
      }
    }
  }

  def signConnectorRequest(origin: Origin, sessionID: SessionID, request: ConnectorRequest): Future[SignedMessage] = {
    val walletDataF = Future.fromTry {
      Try {
        walletData.getOrElse(
          throw new RuntimeException("You need to create the wallet before logging in and creating session")
        )
      }
    }
    val validSessionF = Future.fromTry {
      Try {
        session
          .find(_ == (sessionID -> origin))
          .getOrElse(throw new RuntimeException("You need a valid session to sign the request"))
      }
    }

    for {
      walletData <- walletDataF
      _ <- validSessionF
    } yield {
      val ecKeyPair = ecKeyPairFromSeed(walletData.mnemonic)
      val signedRequest = requestAuthenticator.signConnectorRequest(request.bytes, ecKeyPair.privateKey)
      SignedMessage(
        did = walletData.did,
        didKeyId = ECKeyOperation.firstMasterKeyId,
        base64UrlSignature = signedRequest.encodedSignature,
        base64UrlNonce = signedRequest.encodedRequestNonce
      )
    }
  }

  def verifySignedCredential(
      origin: Origin,
      sessionID: String,
      signedCredentialStringRepresentation: String,
      encodedMerkleProof: String
  ): Future[ValidatedNel[VerificationError, Unit]] = {
    val validSessionF = Future.fromTry {
      Try {
        session
          .find(_ == (sessionID -> origin))
          .getOrElse(throw new RuntimeException("You need a valid session"))
      }
    }

    for {
      _ <- validSessionF
      merkleProof =
        MerkleInclusionProof
          .decode(encodedMerkleProof)
          .getOrElse(throw new RuntimeException(s"Unable to decode merkle proof: $encodedMerkleProof"))
      x <- nodeClientService.verifyCredential(signedCredentialStringRepresentation, merkleProof)
    } yield x
  }

  def requestSignature(origin: Origin, sessionID: SessionID, subject: CredentialSubject): Future[Unit] = {

    Future.fromTry {
      Try {
        session
          .find(_ == (sessionID -> origin))
          .map { _ =>
            val signaturePromise = Promise[String]()
            requestCounter += 1
            val request = SigningRequest(requestCounter, origin, sessionID, subject)
            signingRequests += requestCounter -> ((request, signaturePromise))
            updateBadge()
            reloadPopup()
            signaturePromise.future
          }
          .getOrElse(
            throw new RuntimeException("Invalid request not associated with a session")
          )
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

      aesCtr = crypto.AesDerivedKeyParams(AES_MODE, AES_KEY_LENGTH)
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
      (response, did) <- registerDid(mnemonic, role, organisationName, logo)
      newWalletData = WalletData(
        Map.empty,
        mnemonic,
        organisationName,
        did,
        response.transactionInfo.map(_.transactionId),
        role,
        logo
      )
      _ <- save(aesKey, newWalletData)
      _ = updateWalletData(newWalletData)
    } yield ()

    result.onComplete {
      case Success(_) =>
        val retrievedWalletData = walletData.getOrElse(throw new RuntimeException("Failed to load wallet"))
        subscribeForReceivedCredentials(retrievedWalletData)
        println("Successfully created wallet")

      case Failure(exception) => println("Failed creating wallet"); exception.printStackTrace()
    }

    result
  }

  def recoverWallet(
      password: String,
      mnemonic: Mnemonic
  ): Future[Unit] = {
    val result = for {
      newWalletData <- recoverAccount(mnemonic)
      aesKey <- generateSecretKey(password)
      _ <- save(aesKey, newWalletData)
      _ = updateWalletData(newWalletData)
    } yield ()

    result.onComplete {
      case Success(_) =>
        val retrievedWalletData = walletData.getOrElse(throw new RuntimeException("Failed to load wallet"))
        subscribeForReceivedCredentials(retrievedWalletData)
        println("Successfully recovered wallet")

      case Failure(exception) =>
        println("Failed recovering wallet"); exception.printStackTrace()
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
              .decrypt(initialAesGcm, aesKey, encryptedBytes)
              .toFuture
              .asInstanceOf[Future[ArrayBuffer]]
              .map { buffer =>
                val arr = Array.ofDim[Byte](buffer.byteLength)
                TypedArrayBuffer.wrap(buffer).get(arr)
                new String(arr, "UTF-8")
              }
          }
          .getOrElse(throw new RuntimeException("You need to create the wallet before unlocking it"))
      _ = updateWalletData(parseWalletDataFromJson(json))
    } yield ()

    result.onComplete {
      case Success(_) =>
        val retrievedWalletData = walletData.getOrElse(throw new RuntimeException("Failed to load wallet"))
        subscribeForReceivedCredentials(retrievedWalletData)
        println("Successfully loaded wallet")
      case Failure(exception) =>
        println("Failed loading wallet")
        exception.printStackTrace()
    }

    result
  }

  private def parseWalletDataFromJson(json: String): WalletData = {
    println(s"Parsing wallet data from: $json")
    parse(json)
      .getOrElse(Json.obj())
      .as[WalletData]
      .getOrElse(throw new RuntimeException("Wallet could not be loaded from JSON"))
  }

  def lock(): Future[Unit] = {
    Future {
      credentialsCopyJob.stop()
      this.walletData = None
    }
  }

  private def registerDid(
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[(RegisterDIDResponse, DID)] = {

    val connectRole = Role.toConnectorApiRole(role)
    val logoByteString = ByteString.copyFrom(logo)
    val ecKeyPair = ECKeyOperation.ecKeyPairFromSeed(mnemonic)

    val registerDIDRequest = RegisterDIDRequest(
      Some(signedAtalaOperation(ecKeyPair, createDIDAtalaOperation(ecKeyPair))),
      connectRole,
      organisationName,
      logoByteString
    )

    // use an unpublished DID to start authenticating requests right away
    connectorClientService
      .registerDID(registerDIDRequest)
      .map(_ -> DID.createUnpublishedDID(ecKeyPair.publicKey))
  }

  private def recoverAccount(mnemonic: Mnemonic): Future[WalletData] = {
    val ecKeyPair = ECKeyOperation.ecKeyPairFromSeed(mnemonic)
    val did = didFromMasterKey(ecKeyPair)
    connectorClientService.getCurrentUser(ecKeyPair, did).map { res =>
      WalletData(
        keys = Map.empty,
        mnemonic = mnemonic,
        did = did,
        organisationName = res.name,
        role = Role.toRole(res.role),
        logo = res.logo.bytes
      )
    }
  }

  private def subscribeForReceivedCredentials(data: WalletData): Unit = {
    credentialsCopyJob.start(
      ecKeyPairFromSeed(data.mnemonic),
      data.did
    )
  }
}
