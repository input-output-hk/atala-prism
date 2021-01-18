package io.iohk.atala.prism.management.console

import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.management.console.repositories.{
  ContactsRepository,
  CredentialIssuancesRepository,
  InstitutionGroupsRepository,
  ParticipantsRepository,
  RequestNoncesRepository,
  StatisticsRepository
}
import io.iohk.atala.prism.management.console.services.ConsoleServiceImpl
import io.iohk.atala.prism.protos.console_api
import org.mockito.MockitoSugar.mock

class ManagementConsoleRpcSpecBase extends RpcSpecBase {

  override def services =
    Seq(
      console_api.ConsoleServiceGrpc
        .bindService(
          consoleService,
          executionContext
        )
    )

  lazy val participantsRepository = new ParticipantsRepository(database)(executionContext)
  lazy val requestNoncesRepository = new RequestNoncesRepository.PostgresImpl(database)(executionContext)
  lazy val contactsRepository = new ContactsRepository(database)(executionContext)
  lazy val statisticsRepository = new StatisticsRepository(database)
  lazy val institutionGroupsRepository = new InstitutionGroupsRepository(database)(executionContext)
  lazy val credentialIssuancesRepository = new CredentialIssuancesRepository(database)(executionContext)

  lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val connectorMock =
    mock[io.iohk.atala.prism.protos.connector_api.ContactConnectionServiceGrpc.ContactConnectionService]
  lazy val authenticator =
    new ManagementConsoleAuthenticator(
      participantsRepository,
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  lazy val consoleService = new ConsoleServiceImpl(
    contactsRepository,
    statisticsRepository,
    institutionGroupsRepository,
    credentialIssuancesRepository,
    authenticator,
    connectorMock
  )(
    executionContext
  )

  val usingApiAs: ApiTestHelper[console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub] =
    usingApiAsConstructor(
      new console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub(_, _)
    )
}
