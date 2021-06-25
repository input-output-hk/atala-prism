package io.iohk.atala.prism.vault

import io.iohk.atala.prism.{ApiTestHelper, RpcSpecBase}
import io.iohk.atala.prism.auth.grpc.GrpcAuthenticationHeaderParser
import io.iohk.atala.prism.protos.vault_api
import io.iohk.atala.prism.vault.repositories.{PayloadsRepository, RequestNoncesRepository}
import io.iohk.atala.prism.vault.services.EncryptedDataVaultServiceImpl
import org.mockito.MockitoSugar._

class VaultRpcSpecBase extends RpcSpecBase {

  override def services =
    Seq(
      vault_api.EncryptedDataVaultServiceGrpc
        .bindService(
          vaultService,
          executionContext
        )
    )

  lazy val requestNoncesRepository = RequestNoncesRepository.PostgresImpl(database)
  lazy val payloadsRepository = PayloadsRepository(database)

  lazy val nodeMock = mock[io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService]
  lazy val authenticator =
    new VaultAuthenticator(
      requestNoncesRepository,
      nodeMock,
      GrpcAuthenticationHeaderParser
    )

  lazy val vaultService = new EncryptedDataVaultServiceImpl(
    payloadsRepository,
    authenticator
  )(
    executionContext
  )

  val usingApiAs: ApiTestHelper[vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub] =
    usingApiAsConstructor(
      new vault_api.EncryptedDataVaultServiceGrpc.EncryptedDataVaultServiceBlockingStub(_, _)
    )
}
