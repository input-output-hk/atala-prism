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
import org.scalajs.dom.crypto.CryptoKey

import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

private object WalletManager {

  val LOCAL_STORAGE_KEY = "atala-wallet-data"

  type SessionID = String
  type Origin = String

  case class State(
      sessions: Map[SessionID, Origin],
      wallet: Option[WalletData],
      pendingRequestsQueue: PendingRequestsQueue
  ) {
    def unsafeWallet(): WalletData = {
      wallet.getOrElse(
        throw new RuntimeException("You need to create the wallet before logging in and creating session")
      )
    }
  }

  object State {
    def empty: State = State(Map.empty, None, PendingRequestsQueue.empty)
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

  import WalletManager._

  private val requestAuthenticator = new RequestAuthenticator(EC)

  private[this] var state = State.empty

  private def updateBadge(): Unit = {
    val badgeText = Option(state.pendingRequestsQueue.size)
      .filter(_ > 0)
      .map(_.toString)
      .getOrElse("")
    browserActionService.setBadgeText(badgeText)
  }

  private def updateState(newState: State => State): Unit = {
    this.state = newState(state)
    updateBadge()
  }

  private def reloadPopup(): Unit = {
    browserActionService.reloadPopup()
  }

  private def updateWalletData(walletData: WalletData): Unit = {
    updateState(_.copy(wallet = Some(walletData)))
  }

  def getSigningRequests(): Seq[PendingRequest] = {
    state.pendingRequestsQueue.list
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
      removeRequest(requestId)
    }
  }

  private def removeRequest(requestId: Int): Unit = {
    updateState { cur =>
      cur.copy(pendingRequestsQueue = cur.pendingRequestsQueue - requestId)
    }
  }

  def rejectRequest(requestId: Int): Future[Unit] = {
    issueCredentialRequestF(requestId)
      .map {
        case (request, _) =>
          removeRequest(requestId)
          println(s"Rejected = ${request.subject.id}")
      }
  }

  def getStatus(): Future[WalletStatus] = {
    // Avoid local storage call when the wallet is already loaded
    if (state.wallet.nonEmpty) {
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
        state.unsafeWallet().transactionId.getOrElse(throw new RuntimeException("Transaction Id not found"))
      }
    }
  }

  def getLoggedInUserSession(origin: Origin): Future[UserDetails] = {
    Future.fromTry {
      Try {
        val wallet = state.unsafeWallet()
        // We need to reuse an existing session to avoid breaking the websites while using many tabs
        // otherwise, each tab would keep it's own session, breaking the others.
        val sessionId = state.sessions
          .find(_._2 == origin)
          .map(_._1)
          .getOrElse { UUID.randomUUID().toString }

        updateState { cur =>
          cur.copy(sessions = cur.sessions + (sessionId -> origin))
        }
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
      val (newRequestQueue, promise) = state.pendingRequestsQueue.issueCredential(origin, sessionID, subject)
      updateState(_.copy(pendingRequestsQueue = newRequestQueue))
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
        val retrievedWalletData = state.unsafeWallet()
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
        val retrievedWalletData = state.unsafeWallet()
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
      storedEncryptedBase64Option <-
        storageService
          .load(WalletManager.LOCAL_STORAGE_KEY)
          .map(_.map(_.asInstanceOf[String]))

      json <-
        storedEncryptedBase64Option
          .map { encryptedBase64 =>
            val encryptedBytes = Base64.getDecoder.decode(encryptedBase64)
            CryptoUtils.decrypt(aesKey, encryptedBytes)
          }
          .getOrElse(throw new RuntimeException("You need to create the wallet before unlocking it"))

      walletData <- Future.fromTry(WalletData.fromJson(json))
      _ = updateWalletData(walletData)
    } yield ()

    result.onComplete {
      case Success(_) =>
        val retrievedWalletData = state.unsafeWallet()
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
      updateState(_.copy(wallet = None))
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
      encryptedData <- CryptoUtils.encrypt(key, json)
      base64EncryptedData = Base64.getEncoder.encodeToString(encryptedData)
      _ = println(s"Encrypted wallet: $base64EncryptedData")
      _ = storageService.store(WalletManager.LOCAL_STORAGE_KEY, base64EncryptedData)
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
        state.pendingRequestsQueue
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
        state.unsafeWallet()
      }
    }
  }

  private def validateSessionF(origin: Origin, sessionID: SessionID): Future[Unit] = {
    val t = Try {
      state.sessions
        .find(_ == (sessionID -> origin))
        .map(_ => ())
        .getOrElse(throw new RuntimeException("You need a valid session"))
    }
    Future.fromTry(t)
  }
}
