package io.iohk.atala.cvp.webextension.background.wallet

import cats.data.ValidatedNel
import com.google.protobuf.ByteString
import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.CredentialsCopyJob
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserActionService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.background.services.node.NodeClientService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.background.wallet.models._
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.models._
import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, Mnemonic}
import io.iohk.atala.prism.connector.RequestAuthenticator
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.{RegisterDIDRequest, RegisterDIDResponse}
import org.scalajs.dom.crypto
import org.scalajs.dom.crypto.CryptoKey

import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, _}
import scala.util.{Failure, Success, Try}

private object WalletManager {

  val LOCAL_STORAGE_KEY = "atala-wallet-data"

  type SessionID = String
  type Origin = String
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

  import WalletManager._

  private val requestAuthenticator = new RequestAuthenticator(EC)

  private[this] var session = Set[(SessionID, Origin)]()
  private var walletData: Option[WalletData] = None
  private var pendingRequestsQueue = PendingRequestsQueue.empty

  private def updateBadge(): Unit = {
    val badgeText = Option(pendingRequestsQueue.size)
      .filter(_ > 0)
      .map(_.toString)
      .getOrElse("")
    browserActionService.setBadgeText(badgeText)
  }

  private def updateState(newState: PendingRequestsQueue): Unit = {
    this.pendingRequestsQueue = newState
    updateBadge()
  }

  private def reloadPopup(): Unit = {
    browserActionService.reloadPopup()
  }

  private def updateWalletData(walletData: WalletData): Unit = {
    this.walletData = Some(walletData)
  }

  def getSigningRequests(): Seq[PendingRequest] = {
    pendingRequestsQueue.list
  }

  // TODO: Support credential revocation requests
  def signRequestAndPublish(requestId: Int): Future[Unit] = {
    for {
      (request, _) <- issueCredentialRequestF(requestId)
      walletData <- walletDataF()
      ecKeyPair = ECKeyOperation.ecKeyPairFromSeed(walletData.mnemonic)
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
      println(s"Signed and Published = ${request.subject.id}")
      updateState(pendingRequestsQueue - requestId)
    }
  }

  def rejectRequest(requestId: Int): Future[Unit] = {
    issueCredentialRequestF(requestId)
      .map {
        case (request, _) =>
          updateState(pendingRequestsQueue - requestId)
          println(s"Rejected = ${request.subject.id}")
      }
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
    for {
      walletData <- walletDataF()
      _ <- validateSessionF(origin = origin, sessionID = sessionID)
    } yield {
      val ecKeyPair = ECKeyOperation.ecKeyPairFromSeed(walletData.mnemonic)
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
    for {
      _ <- validateSessionF(origin = origin, sessionID = sessionID)
      merkleProof =
        MerkleInclusionProof
          .decode(encodedMerkleProof)
          .getOrElse(throw new RuntimeException(s"Unable to decode merkle proof: $encodedMerkleProof"))
      x <- nodeClientService.verifyCredential(signedCredentialStringRepresentation, merkleProof)
    } yield x
  }

  def requestSignature(origin: Origin, sessionID: SessionID, subject: CredentialSubject): Future[Unit] = {
    for {
      _ <- validateSessionF(origin = origin, sessionID = sessionID)
    } yield {
      val (newState, promise) = pendingRequestsQueue.issueCredential(origin, sessionID, subject)
      updateState(newState)
      reloadPopup()
      // TODO: Avoid ignoring the future result
      println(s"Future ${promise.future} ignored on purpose to keep compatibility, be kind and fix the bug")
      ()
    }
  }

  def createWallet(
      password: String,
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[Unit] = {
    val result = for {
      aesKey <- CryptoUtils.generateSecretKey(password)
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

  def recoverWallet(password: String, mnemonic: Mnemonic): Future[Unit] = {
    val result = for {
      newWalletData <- recoverAccount(mnemonic)
      aesKey <- CryptoUtils.generateSecretKey(password)
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
      aesKey <- CryptoUtils.generateSecretKey(password)
      storedEncryptedJsonOption <- storageService.load(WalletManager.LOCAL_STORAGE_KEY)
      json <-
        storedEncryptedJsonOption
          .map { storedEncryptedJson =>
            val encryptedJson = storedEncryptedJson.asInstanceOf[String]
            val encryptedBytes = Base64.getDecoder.decode(encryptedJson.asInstanceOf[String]).toTypedArray.buffer
            crypto.crypto.subtle
              .decrypt(CryptoUtils.initialAesGcm, aesKey, encryptedBytes)
              .toFuture
              .asInstanceOf[Future[ArrayBuffer]]
              .map { buffer =>
                val arr = Array.ofDim[Byte](buffer.byteLength)
                TypedArrayBuffer.wrap(buffer).get(arr)
                new String(arr, "UTF-8")
              }
          }
          .getOrElse(throw new RuntimeException("You need to create the wallet before unlocking it"))

      walletData <- Future.fromTry(WalletData.fromJson(json))
      _ = updateWalletData(walletData)
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

  def lock(): Future[Unit] = {
    val t = Try {
      credentialsCopyJob.stop()
      this.walletData = None
    }
    Future.fromTry(t)
  }

  private def registerDid(
      mnemonic: Mnemonic,
      role: Role,
      organisationName: String,
      logo: Array[Byte]
  ): Future[(RegisterDIDResponse, DID)] = {

    val connectRole = RoleHepler.toConnectorApiRole(role)
    val logoByteString = ByteString.copyFrom(logo)
    val ecKeyPair = ECKeyOperation.ecKeyPairFromSeed(mnemonic)

    val createDIDOperation = ECKeyOperation.createDIDAtalaOperation(ecKeyPair)
    val registerDIDRequest = RegisterDIDRequest(
      Some(ECKeyOperation.signedAtalaOperation(ecKeyPair, createDIDOperation)),
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
    val did = ECKeyOperation.didFromMasterKey(ecKeyPair)
    connectorClientService.getCurrentUser(ecKeyPair, did).map { res =>
      WalletData(
        keys = Map.empty,
        mnemonic = mnemonic,
        did = did,
        organisationName = res.name,
        role = RoleHepler.toRole(res.role),
        logo = res.logo.bytes
      )
    }
  }

  private def subscribeForReceivedCredentials(data: WalletData): Unit = {
    credentialsCopyJob.start(
      ECKeyOperation.ecKeyPairFromSeed(data.mnemonic),
      data.did
    )
  }

  private def save(key: CryptoKey, walletData: WalletData): Future[Unit] = {
    val json = walletData.asJson.noSpaces
    println(s"Serialized wallet: $json")
    val result = for {
      arrayBuffer <-
        crypto.crypto.subtle
          .encrypt(CryptoUtils.initialAesGcm, key, json.getBytes.toTypedArray.buffer)
          .toFuture
          .asInstanceOf[Future[ArrayBuffer]]
      arr = Array.ofDim[Byte](arrayBuffer.byteLength)
      _ = TypedArrayBuffer.wrap(arrayBuffer).get(arr)
      encodedJson = new String(Base64.getEncoder.encode(arr))
      _ = println(s"Encrypted wallet: $encodedJson")
      _ = storageService.store(WalletManager.LOCAL_STORAGE_KEY, encodedJson)
    } yield ()

    result.onComplete {
      case Success(_) => println("Successfully saved wallet")
      case Failure(exception) => println("Failed saving wallet"); exception.printStackTrace()
    }

    result
  }

  private def issueCredentialRequestF(requestId: Int): Future[(PendingRequest.IssueCredential, Promise[String])] = {
    Future.fromTry {
      Try {
        pendingRequestsQueue
          .get(requestId)
          .map {
            case (request, promise) =>
              request match {
                case r: PendingRequest.IssueCredential => r -> promise
                case _ =>
                  throw new RuntimeException("Request to issue credential expected but a different one was found")
              }
          }
          .getOrElse(throw new RuntimeException("Request to issue credential not found"))
      }
    }
  }

  private def walletDataF(): Future[WalletData] = {
    Future.fromTry {
      Try {
        walletData.getOrElse(
          throw new RuntimeException("You need to create/unlock the wallet before being able to use it")
        )
      }
    }
  }

  private def validateSessionF(origin: Origin, sessionID: SessionID): Future[Unit] = {
    val t = Try {
      session
        .find(_ == (sessionID -> origin))
        .map(_ => ())
        .getOrElse(throw new RuntimeException("You need a valid session"))
    }
    Future.fromTry(t)
  }
}
