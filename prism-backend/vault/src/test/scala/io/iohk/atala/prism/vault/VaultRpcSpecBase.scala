package io.iohk.atala.prism.vault

import cats.effect.{ContextShift, IO}
import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.grpc.EncryptedDataVaultGRPCService
import io.iohk.atala.prism.vault.repositories.{PayloadsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.vault.services.EncryptedDataVaultService
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
  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  private lazy val vaultTestLogs: Logs[IO, IO] = Logs.sync[IO, IO]

  lazy val requestNoncesRepository = vaultTestLogs
    .forService[RequestNoncesRepository[IO]]
    .map(implicit l => RequestNoncesRepository.PostgresImpl.create(database))
    .unsafeRunSync()
  lazy val payloadsRepository = vaultTestLogs
    .forService[PayloadsRepository[IO]]
    .map(implicit l => PayloadsRepository.create(database))
    .unsafeRunSync()

  lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val authenticator =
    new VaultAuthenticator(
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  lazy val encryptedDataVaultService = vaultTestLogs
    .forService[EncryptedDataVaultService[IO]]
    .map(implicit l => EncryptedDataVaultService.create(payloadsRepository))
    .unsafeRunSync()

  lazy val vaultGrpcService = new EncryptedDataVaultGRPCService(
    encryptedDataVaultService,
    authenticator
  )(
    executionContext
  )

  val usingApiAs: ApiTestHelper[vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub] =
    usingApiAsConstructor(
      new vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub(_, _)
    )
}
