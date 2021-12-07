package io.iohk.atala.prism.vault

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.iohk.atala.prism.auth.AuthenticatorF
import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import io.iohk.atala.prism.logging.TraceId.IOWithTraceIdContext
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.grpc.EncryptedDataVaultGrpcService
import io.iohk.atala.prism.vault.repositories.RecordsRepository
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
import io.iohk.atala.prism.utils.IOUtils._
import org.mockito.MockitoSugar._
import tofu.logging.Logs

class VaultRpcSpecBase extends RpcSpecBase {

  override def services =
    Seq(
      vault_api.EncryptedDataVaultServiceGrpc
        .bindService(
          vaultGrpcService,
          executionContext
        )
    )
  private val vaultTestLogs: Logs[IO, IOWithTraceIdContext] =
    Logs.withContext[IO, IOWithTraceIdContext]

  lazy val (recordsRepository, vaultGrpcService) = (for {
    recordsRepository <- RecordsRepository.create(dbLiftedToTraceIdIO, vaultTestLogs)
    nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
    authenticator = AuthenticatorF.unsafe(
      nodeMock,
      new VaultAuthenticator,
      vaultTestLogs
    )
    encryptedDataVaultService <- EncryptedDataVaultService.create(recordsRepository, vaultTestLogs)
    vaultGrpcService = new EncryptedDataVaultGrpcService(
      encryptedDataVaultService,
      authenticator
    )(
      executionContext,
      global
    )
  } yield (recordsRepository, vaultGrpcService)).unsafeRunSync()

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
