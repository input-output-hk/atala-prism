package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.console.DataPreparation.{createContact, createIssuer, createIssuerGroup}
import io.iohk.atala.prism.console.models.IssuerGroup
import io.iohk.atala.prism.console.repositories.StatisticsRepository
import io.iohk.atala.prism.crypto.EC
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.ConsoleServiceGrpc
import io.iohk.atala.prism.{DIDGenerator, RpcSpecBase}
import org.mockito.MockitoSugar._

class ConsoleServiceImplSpec extends RpcSpecBase with DIDGenerator {
  private val usingApiAs = usingApiAsConstructor(new ConsoleServiceGrpc.ConsoleServiceBlockingStub(_, _))

  private lazy val participantsRepository = new ParticipantsRepository(database)
  private lazy val statisticsRepository = new StatisticsRepository(database)
  private lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  private lazy val authenticator = new ConnectorAuthenticator(
    participantsRepository,
    requestNoncesRepository,
    nodeMock,
    GrpcAuthenticationHeaderParser
  )

  override def services =
    Seq(
      console_api.ConsoleServiceGrpc
        .bindService(
          new ConsoleServiceImpl(statisticsRepository, authenticator),
          executionContext
        )
    )

  "getStatistics" should {
    "work" in {
      val issuerName = "tokenizer"
      val groupName = IssuerGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val keyPair = EC.generateKeyPair()
      val publicKey = keyPair.publicKey
      val did = generateDid(publicKey)
      val issuerId = createIssuer(issuerName, publicKey = Some(publicKey), did = Some(did))
      createIssuerGroup(issuerId, groupName)
      val _ = createContact(issuerId, contactName, groupName)
      val request = console_api.GetStatisticsRequest()
      val rpcRequest = SignedRpcRequest.generate(keyPair, did, request)

      usingApiAs(rpcRequest) { serviceStub =>
        val response = serviceStub.getStatistics(request)
        response.numberOfContacts must be(1)
        response.numberOfContactsConnected must be(0)
        response.numberOfContactsPendingConnection must be(0)
        response.numberOfGroups must be(1)
        response.numberOfCredentialsInDraft must be(0)
        response.numberOfCredentialsPublished must be(0)
        response.numberOfCredentialsReceived must be(0)
      }
    }
  }
}
