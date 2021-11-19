package io.iohk.atala.prism.management.console

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.management.console.clients.ConnectorClient
import io.iohk.atala.prism.management.console.grpc.{
  ConsoleGrpcService,
  ContactsGrpcService,
  CredentialIssuanceGrpcService,
  CredentialTypesGrpcService,
  CredentialsGrpcService
}
import io.iohk.atala.prism.management.console.integrations.{
  ContactsIntegrationService,
  CredentialsIntegrationService,
  ParticipantsIntegrationService
}
import io.iohk.atala.prism.management.console.repositories._
import io.iohk.atala.prism.management.console.services._
import io.iohk.atala.prism.protos.console_api
import io.iohk.atala.prism.protos.console_api.CredentialIssuanceServiceGrpc
import io.iohk.atala.prism.utils.IOUtils._
import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import org.mockito.MockitoSugar.mock
import tofu.logging.Logs

class ManagementConsoleRpcSpecBase extends RpcSpecBase {
  private val managementConsoleTestLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

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
        ),
      console_api.CredentialTypesServiceGrpc
        .bindService(
          credentialTypeService,
          executionContext
        ),
      console_api.CredentialsServiceGrpc
        .bindService(
          credentialsService,
          executionContext
        )
    )
  }

  lazy val participantsRepository = ParticipantsRepository.unsafe(
    dbLiftedToTraceIdIO,
    managementConsoleTestLogs
  )
  lazy val requestNoncesRepository = RequestNoncesRepository.unsafe(
    dbLiftedToTraceIdIO,
    managementConsoleTestLogs
  )
  lazy val contactsRepository =
    ContactsRepository.unsafe(dbLiftedToTraceIdIO, managementConsoleTestLogs)
  lazy val statisticsRepository =
    StatisticsRepository.unsafe(dbLiftedToTraceIdIO, managementConsoleTestLogs)
  lazy val institutionGroupsRepository =
    InstitutionGroupsRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  lazy val credentialIssuancesRepository =
    CredentialIssuancesRepository.unsafe(
      dbLiftedToTraceIdIO,
      managementConsoleTestLogs
    )
  lazy val credentialsRepository =
    CredentialsRepository.unsafe(dbLiftedToTraceIdIO, managementConsoleTestLogs)
  lazy val credentialTypeRepository = CredentialTypeRepository.unsafe(
    dbLiftedToTraceIdIO,
    managementConsoleTestLogs
  )

  lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]

  lazy val authenticator = AuthenticatorF.unsafe(
    nodeMock,
    new ManagementConsoleAuthenticator(
      participantsRepository,
      requestNoncesRepository
    ),
    managementConsoleTestLogs
  )

  lazy val connectorMock = mock[ConnectorClient[IOWithTraceIdContext]]
  lazy val contactsIntegrationService =
    ContactsIntegrationService.unsafe(
      contactsRepository,
      connectorMock,
      managementConsoleTestLogs
    )

  lazy val participantsIntegrationService =
    ParticipantsIntegrationService.unsafe(
      participantsRepository,
      managementConsoleTestLogs
    )
  lazy val consoleService = new ConsoleGrpcService(
    ConsoleService.unsafe(
      participantsIntegrationService,
      statisticsRepository,
      managementConsoleTestLogs
    ),
    authenticator
  )
  lazy val contactsService =
    new ContactsGrpcService(contactsIntegrationService, authenticator)
  lazy val credentialIssuanceService = new CredentialIssuanceGrpcService(
    CredentialIssuanceService
      .unsafe(credentialIssuancesRepository, managementConsoleTestLogs),
    authenticator
  )

  lazy val credentialTypeService = new CredentialTypesGrpcService(
    CredentialTypesService
      .unsafe(credentialTypeRepository, managementConsoleTestLogs),
    authenticator
  )

  lazy val credentialsIntegrationService =
    CredentialsIntegrationService.unsafe(
      credentialsRepository,
      nodeMock,
      connectorMock,
      managementConsoleTestLogs
    )
  lazy val credentialsService =
    new CredentialsGrpcService(
      CredentialsService
        .unsafe(
          credentialsRepository,
          credentialsIntegrationService,
          nodeMock,
          connectorMock,
          managementConsoleTestLogs
        ),
      authenticator
    )

  val usingApiAsConsole: ApiTestHelper[
    console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub
  ] = {
    usingApiAsConstructor(
      new console_api.ConsoleServiceGrpc.ConsoleServiceBlockingStub(_, _)
    )
  }

  val usingApiAsContacts: ApiTestHelper[
    console_api.ContactsServiceGrpc.ContactsServiceBlockingStub
  ] = {
    usingApiAsConstructor(
      new console_api.ContactsServiceGrpc.ContactsServiceBlockingStub(_, _)
    )
  }

  val usingApiAsCredentialIssuance: ApiTestHelper[
    console_api.CredentialIssuanceServiceGrpc.CredentialIssuanceServiceBlockingStub
  ] = {
    usingApiAsConstructor(
      new CredentialIssuanceServiceGrpc.CredentialIssuanceServiceBlockingStub(
        _,
        _
      )
    )
  }

  val usingApiAsCredentialType: ApiTestHelper[
    console_api.CredentialTypesServiceGrpc.CredentialTypesServiceBlockingStub
  ] = {
    usingApiAsConstructor(
      new console_api.CredentialTypesServiceGrpc.CredentialTypesServiceBlockingStub(
        _,
        _
      )
    )
  }

  val usingApiAsCredentials: ApiTestHelper[
    console_api.CredentialsServiceGrpc.CredentialsServiceBlockingStub
  ] = {
    usingApiAsConstructor(
      new console_api.CredentialsServiceGrpc.CredentialsServiceBlockingStub(
        _,
        _
      )
    )
  }
}
