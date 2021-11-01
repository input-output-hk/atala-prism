package io.iohk.atala.prism.vault

import cats.effect.IO
import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.grpc.EncryptedDataVaultGrpcService
import io.iohk.atala.prism.vault.repositories.{PayloadsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
import io.iohk.atala.prism.utils.IOUtils._
import org.mockito.MockitoSugar._
import tofu.logging.Logs

import scala.concurrent.ExecutionContext

class VaultRpcSpecBase extends RpcSpecBase {

  override def services =
    Seq(
      vault_api.EncryptedDataVaultServiceGrpc
        .bindService(
          vaultGrpcService,
          executionContext
        )
    )
  implicit lazy val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  private val vaultTestLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  lazy val requestNoncesRepository = RequestNoncesRepository.PostgresImpl.unsafe(dbLiftedToTraceIdIO, vaultTestLogs)
  lazy val payloadsRepository = PayloadsRepository.unsafe(dbLiftedToTraceIdIO, vaultTestLogs)

  lazy val nodeMock =
    mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val authenticator =
    new VaultAuthenticator(
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  lazy val encryptedDataVaultService = EncryptedDataVaultService.unsafe(payloadsRepository, vaultTestLogs)

  lazy val vaultGrpcService = new EncryptedDataVaultGrpcService(
    encryptedDataVaultService,
    authenticator
  )(
    executionContext
  )

  val usingApiAs: ApiTestHelper[
    vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub
  ] =
    usingApiAsConstructor(
      new vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub(
        _,
        _
      )
    )
}
