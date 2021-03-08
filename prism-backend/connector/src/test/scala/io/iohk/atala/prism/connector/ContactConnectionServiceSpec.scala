package io.iohk.atala.prism.connector

import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.model.ConnectionStatus
import io.iohk.atala.prism.connector.repositories._
import io.iohk.atala.prism.connector.services.{ConnectionsService, ContactConnectionService}
import io.iohk.atala.prism.console.DataPreparation
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.{connector_api, connector_models, console_models}
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar.mock

class ContactConnectionServiceSpec extends RpcSpecBase with DIDGenerator with ConnectorRepositorySpecBase {
  private val usingApiAs = usingApiAsConstructor(
    new connector_api.ContactConnectionServiceGrpc.ContactConnectionServiceBlockingStub(_, _)
  )

  protected lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  private lazy val connectionsRepository = new ConnectionsRepository.PostgresImpl(database)(executionContext)
  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)

  val keyPair = EC.generateKeyPair()
  val publicKey = keyPair.publicKey
  val did = DID.createUnpublishedDID(publicKey)

  lazy val connectionsService = new ConnectionsService(connectionsRepository, nodeMock)

  private lazy val authenticator =
    new ConnectorAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  override def services =
    Seq(
      connector_api.ContactConnectionServiceGrpc
        .bindService(new ContactConnectionService(connectionsService, authenticator, Set(did)), executionContext)
    )

  "ContactConnectionService" should {
    "return correct connection status for acceptors" in {
      DataPreparation.createIssuer("Issuer", "", Some(publicKey), Some(did))

      val initiator1 = createHolder("initiator1", None)
      val acceptor1 = createHolder("acceptor1", None)
      val token1 = createToken(initiator1)
      val connectionId1 = createConnection(initiator1, acceptor1, token1, ConnectionStatus.InvitationMissing)

      val initiator2 = createHolder("initiator2", None)
      val acceptor2 = createHolder("acceptor2", None)
      val token2 = createToken(initiator2)
      val connectionId2 = createConnection(initiator2, acceptor2, token2, ConnectionStatus.ConnectionAccepted)

      val contactConnection1 = connector_models.ContactConnection(
        connectionId1.toString,
        token1.token,
        console_models.ContactConnectionStatus.STATUS_INVITATION_MISSING
      )
      val contactConnection2 = connector_models.ContactConnection(
        connectionId2.toString,
        token2.token,
        console_models.ContactConnectionStatus.STATUS_CONNECTION_ACCEPTED
      )

      val request = connector_api.ConnectionsStatusRequest(List(token1.token, token2.token))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { service =>
        service.getConnectionStatus(request).connections must be(List(contactConnection1, contactConnection2))
      }
    }

    "return invitation missing for non-existing connections" in {
      DataPreparation.createIssuer("Issuer", "", Some(publicKey), Some(did))

      val acceptor1 = createHolder("acceptor1", None)
      val acceptor2 = createHolder("acceptor2", None)

      val contactConnection1 =
        connector_models.ContactConnection(connectionStatus =
          console_models.ContactConnectionStatus.STATUS_INVITATION_MISSING
        )
      val contactConnection2 =
        connector_models.ContactConnection(connectionStatus =
          console_models.ContactConnectionStatus.STATUS_INVITATION_MISSING
        )

      val request = connector_api.ConnectionsStatusRequest(List(acceptor1.uuid.toString, acceptor2.uuid.toString))
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { service =>
        service.getConnectionStatus(request).connections must be(List(contactConnection1, contactConnection2))
      }
    }
  }
}
