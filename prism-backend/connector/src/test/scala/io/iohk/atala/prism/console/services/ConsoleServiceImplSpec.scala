package io.iohk.atala.prism.console.services

import io.iohk.atala.prism.auth.SignedRpcRequest
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.connector.ConnectorAuthenticator
import io.iohk.atala.prism.connector.repositories.{ParticipantsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.console.DataPreparation.{createContact, createIssuer, createIssuerGroup}
import io.iohk.atala.prism.console.models.IssuerGroup
import io.iohk.atala.prism.console.repositories.StatisticsRepository
import io.iohk.atala.prism.crypto.ECKeyPair
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.ConsoleServiceGrpc
import io.iohk.atala.prism.{DIDUtil, RpcSpecBase}
import org.mockito.MockitoSugar._
import org.scalatest.Assertion

class ConsoleServiceImplSpec extends RpcSpecBase with DIDUtil {
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
      val (keyPair, did) = createDid
      testGetStatistics(issuerName, groupName, contactName, keyPair, did)
    }
    "work with unpublished did" in {
      val issuerName = "tokenizer"
      val groupName = IssuerGroup.Name("Grp 1")
      val contactName = "Contact 1"
      val (keyPair, did) = DIDUtil.createUnpublishedDid
      testGetStatistics(issuerName, groupName, contactName, keyPair, did)
    }

    def testGetStatistics(
        issuerName: String,
        groupName: IssuerGroup.Name,
        contactName: String,
        keyPair: ECKeyPair,
        did: DID
    ): Assertion = {
      val issuerId = createIssuer(issuerName, publicKey = Some(keyPair.publicKey), did = Some(did))
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
