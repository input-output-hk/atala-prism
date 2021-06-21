package io.iohk.atala.cvp.webextension.background.wallet

import cats.data.ValidatedNel
import cats.syntax.functor._
import io.circe.generic.auto._
import io.circe.syntax._
import io.iohk.atala.cvp.webextension.background.CredentialsCopyJob
import io.iohk.atala.cvp.webextension.background.models.console.ConsoleCredentialId
import io.iohk.atala.cvp.webextension.background.services.browser.BrowserActionService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService
import io.iohk.atala.cvp.webextension.background.services.connector.ConnectorClientService.CredentialData
import io.iohk.atala.cvp.webextension.background.services.console.ConsoleClientService
import io.iohk.atala.cvp.webextension.background.services.node.NodeClientService
import io.iohk.atala.cvp.webextension.background.services.storage.StorageService
import io.iohk.atala.cvp.webextension.background.wallet.models.PendingRequestsQueue.RequestId
import io.iohk.atala.cvp.webextension.background.wallet.models._
import io.iohk.atala.cvp.webextension.circe._
import io.iohk.atala.cvp.webextension.common.models.PendingRequest.{
  IssueCredential,
  IssueCredentialWithId,
  RevokeCredentialWithId
}
import io.iohk.atala.cvp.webextension.common.models._
import io.iohk.atala.cvp.webextension.common.{ECKeyOperation, Mnemonic}
import io.iohk.atala.prism.connector.{AtalaOperationId, RequestAuthenticator, RequestNonce}
import io.iohk.atala.prism.credentials.VerificationError
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.crypto.MerkleTree.MerkleInclusionProof
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.RegisterDIDResponse
import io.iohk.atala.prism.protos.console_api.PublishBatchResponse
import org.scalajs.dom.crypto.CryptoKey

import java.util.{Base64, UUID}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal
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
    consoleClientService: ConsoleClientService,
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

  def getRequestsRequiringManualApproval(): Seq[PendingRequest.WithId] = {
    state.pendingRequestsQueue.list
  }

  def getCredentialIssuanceRequestsRequiringManualApproval(): Seq[IssueCredentialWithId] = {
    state.pendingRequestsQueue.issuanceCredentialRequests
  }

  def getRevocationRequestsRequiringManualApproval(): Seq[RevokeCredentialWithId] = {
    state.pendingRequestsQueue.revocationRequests
  }

  def approveAllCredentialRequests(): Future[Unit] = {
    for {
      walletData <- walletDataF()
      issuingECKeyPair = ECKeyOperation.issuingECKeyPairFromSeed(walletData.mnemonic)
      masterECKeyPair = ECKeyOperation.masterECKeyPairFromSeed(walletData.mnemonic)
      issueCredentials = state.pendingRequestsQueue.issuanceCredentialRequests.map(_.request)
      requestIds = state.pendingRequestsQueue.issuanceCredentialRequests.map(_.id)
      _ <-
        connectorClientService
          .signAndPublishBatch(
            issuingECKeyPair,
            masterECKeyPair,
            walletData.did,
            ECKeyOperation.issuingKeyId,
            toCredentialData(issueCredentials)
          )
          .map(handleSignAndPublishResponse)
      _ <- removeAllCredentialRequestsF(requestIds)
    } yield {
      println(s"Credential Requests batch size ${requestIds.size} approved")
    }
  }

  def approvePendingRequest(requestId: Int): Future[Unit] = {
    for {
      (request, promise) <- requestF(requestId)
      walletData <- walletDataF()
      issuingECKeyPair = ECKeyOperation.issuingECKeyPairFromSeed(walletData.mnemonic)
      masterECKeyPair = ECKeyOperation.masterECKeyPairFromSeed(walletData.mnemonic)
      requestResult <- request match {
        case r: PendingRequest.IssueCredential =>
          // TODO: Issue a batch instead
          val claims = r.credentialData.properties.asJson.noSpaces
          connectorClientService
            .signAndPublishBatch(
              issuingECKeyPair,
              masterECKeyPair,
              walletData.did,
              ECKeyOperation.issuingKeyId,
              List(CredentialData(ConsoleCredentialId(r.credentialData.id), claims))
            )
            .map(handleSignAndPublishResponse)

        case r: PendingRequest.RevokeCredential =>
          // TODO: Decode the credential before accepting the request to make sure it has the proper data (and html view)
          connectorClientService
            .revokeCredential(
              issuingECKeyPair,
              masterECKeyPair,
              walletData.did,
              signedCredentialStringRepresentation = r.signedCredentialStringRepresentation,
              batchId = r.batchId,
              batchOperationHash = r.batchOperationHash,
              credentialId = r.credentialId
            )
            .map { operationIdBytes =>
              AtalaOperationId.fromVectorUnsafe(operationIdBytes.toVector)
            }
      }
      _ = promise.success(requestResult.hexValue)
    } yield {
      println(s"Request approved: $requestId")
      removeRequest(requestId)
    }
  }

  def rejectAllCredentialRequests(): Future[Unit] = {
    val requestIds = state.pendingRequestsQueue.issuanceCredentialRequests.map(_.id)
    removeAllCredentialRequestsF(requestIds)
  }

  def rejectRequest(requestId: Int): Future[Unit] =
    for {
      (_, promise) <- requestF(requestId)
      _ = promise.failure(new RuntimeException("The user rejected the request"))
    } yield {
      removeRequest(requestId)
      println(s"Request rejected = $requestId")
    }

  private def handleSignAndPublishResponse(in: PublishBatchResponse): AtalaOperationId =
    if (in.operationId.isEmpty) {
      throw new RuntimeException("Operation Info Not Returned")
    } else {
      AtalaOperationId.fromVectorUnsafe(in.operationId.toVector)
    }

  private def removeRequest(requestId: Int): Unit = {
    updateState { cur =>
      cur.copy(pendingRequestsQueue = cur.pendingRequestsQueue - requestId)
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

  def getOperationId(): Future[String] = {
    Future.fromTry {
      Try {
        state.unsafeWallet().operationId.getOrElse(throw new RuntimeException("Operation Id not found"))
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

  def signConnectorRequest(
      origin: Origin,
      sessionID: SessionID,
      request: ConnectorRequest,
      nonce: Option[Array[Byte]]
  ): Future[SignedMessage] = {
    for {
      walletData <- walletDataF()
      _ <- validateSessionF(origin = origin, sessionID = sessionID)
    } yield {
      val ecKeyPair = ECKeyOperation.masterECKeyPairFromSeed(walletData.mnemonic)
      val signedRequest = requestAuthenticator.signConnectorRequest(
        request.bytes,
        ecKeyPair.privateKey,
        nonce.map(RequestNonce(_)).getOrElse(RequestNonce())
      )
      SignedMessage(
        did = walletData.did,
        didKeyId = ECKeyOperation.masterKeyId,
        base64UrlSignature = signedRequest.encodedSignature,
        base64UrlNonce = signedRequest.encodedRequestNonce
      )
    }
  }

  // TODO: Report reasonable errors instead of missing fields from the node response
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

  // enqueues a request which should be reviewed manually
  def enqueueRequestApproval(origin: Origin, sessionID: SessionID, request: PendingRequest): Future[String] =
    for {
      _ <- validateSessionF(origin = origin, sessionID = sessionID)
      (newRequestQueue, _) = state.pendingRequestsQueue.enqueue(request)
      _ = updateState(_.copy(pendingRequestsQueue = newRequestQueue))
      _ = reloadPopup()
      result <- Future.successful("Ack")
    } yield result

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
        Some(response.operationId.toStringUtf8()),
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
    val ecKeyPair = ECKeyOperation.masterECKeyPairFromSeed(mnemonic)

    val createDIDOperation = ECKeyOperation.createDIDAtalaOperation(mnemonic)
    val signedOperation = ECKeyOperation.signedAtalaOperation(ECKeyOperation.masterKeyId, ecKeyPair, createDIDOperation)
    val did = ECKeyOperation.unpublishedDidFromMnemonic(mnemonic)

    // Right now, the console and the connector needs to keep the DID registered, while the frontend is migrated
    // to use the console backend, we may not use it, which means that the console registration fails, in that case
    // we just keep the previous behavior which is registering in the connector only.
    //
    // The console is invoked after the connector because the DID gets published by the connector while the
    // console expects that the DID will be already published.
    for {
      response <- {
        connectorClientService
          .registerDID(signedOperation, organisationName, logo, role)
      }
      _ <- {
        consoleClientService
          .registerDID(did, organisationName, logo)
          .void
          .recover {
            case NonFatal(ex) =>
              println(
                s"Failed to register DID on the console, we assume it isn't used for this deployment: ${ex.getMessage}"
              )
          }
      }
    } yield {
      // use an unpublished DID to start authenticating requests right away
      response -> did
    }
  }

  private def recoverAccount(mnemonic: Mnemonic): Future[WalletData] = {
    val did = ECKeyOperation.unpublishedDidFromMnemonic(mnemonic)
    val masterECKeyPair = ECKeyOperation.masterECKeyPairFromSeed(mnemonic)
    connectorClientService.getCurrentUser(masterECKeyPair, did).map { res =>
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
      ECKeyOperation.masterECKeyPairFromSeed(data.mnemonic),
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

  private def toCredentialData(issueCredentials: List[IssueCredential]): List[CredentialData] = {
    issueCredentials.map { req =>
      val data = req.credentialData
      val claims = data.properties.asJson.noSpaces
      CredentialData(ConsoleCredentialId(data.id), claims)
    }
  }

  private def removeAllCredentialRequestsF(requestIds: List[RequestId]): Future[Unit] = {
    Future.fromTry {
      Try {
        updateState { currentState =>
          currentState.copy(pendingRequestsQueue = state.pendingRequestsQueue.removeAll(requestIds))
        }
      }
    }
  }

  private def requestF(requestId: Int): Future[(PendingRequest, Promise[String])] = {
    Future.fromTry {
      Try {
        state.pendingRequestsQueue
          .get(requestId)
          .map {
            case (taggedRequest, promise) => (taggedRequest.request, promise)
          }
          .getOrElse(throw new RuntimeException(s"Request $requestId not found"))
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
        .void
        .getOrElse(throw new RuntimeException("You need a valid session"))
    }
    Future.fromTry(t)
  }
}
