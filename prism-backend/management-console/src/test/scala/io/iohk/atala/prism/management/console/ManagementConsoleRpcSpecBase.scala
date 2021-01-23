package io.iohk.atala.prism.management.console

import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.management.console.integrations.ContactsIntegrationService
import io.iohk.atala.prism.management.console.repositories._
import io.iohk.atala.prism.management.console.services.{
  ConsoleServiceImpl,
  ContactsServiceImpl,
  CredentialIssuanceServiceImpl
}
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.CredentialIssuanceServiceGrpc
import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import org.mockito.MockitoSugar.mock

class ManagementConsoleRpcSpecBase extends RpcSpecBase {

  override def services = {
    Seq(
      console_api.ConsoleServiceGrpc
        .bindService(
          consoleService,
          executionContext
        ),
      console_api.ContactsServiceGrpc
        .bindService(
          contactsService,
          executionContext
        ),
      console_api.CredentialIssuanceServiceGrpc
        .bindService(
          credentialIssuanceService,
          executionContext
        )
    )
  }

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

  lazy val contactsIntegrationService = new ContactsIntegrationService(contactsRepository, connectorMock)

  lazy val consoleService = new ConsoleServiceImpl(statisticsRepository, authenticator)(
    executionContext
  )
  lazy val contactsService = new ContactsServiceImpl(contactsIntegrationService, authenticator)(
    executionContext
  )
  lazy val credentialIssuanceService = new CredentialIssuanceServiceImpl(
    contactsRepository,
    institutionGroupsRepository,
    credentialIssuancesRepository,
    authenticator
  )(
    executionContext
  )

  val usingApiAsConsole: ApiTestHelper[console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub] = {
    usingApiAsConstructor(
      new console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub(_, _)
    )
  }

  val usingApiAsContacts: ApiTestHelper[console_api.ContactsServiceGrpc.ContactsServiceBlockingStub] = {
    usingApiAsConstructor(
      new console_api.ContactsServiceGrpc.ContactsServiceBlockingStub(_, _)
    )
  }

  val usingApiAsCredentialIssuance
      : ApiTestHelper[console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceServiceBlockingStub] = {
    usingApiAsConstructor(
      new CredentialIssuanceServiceGrpc.CredentialIssuanceServiceBlockingStub(_, _)
    )
  }
}
