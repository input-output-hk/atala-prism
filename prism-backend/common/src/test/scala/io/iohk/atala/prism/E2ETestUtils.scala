package io.iohk.atala.prism

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.EC.{INSTANCE => EC}
import io.iohk.atala.prism.crypto.keys.ECKeyPair
import io.iohk.atala.prism.identity.{PrismDid => DID}
import io.iohk.atala.prism.protos.connector_api.{AddConnectionFromTokenRequest, RegisterDIDRequest}
import io.iohk.atala.prism.protos.connector_models.EncodedPublicKey
import io.iohk.atala.prism.protos.node_models
import io.iohk.atala.prism.protos.node_models.{
  AtalaOperation,
  CreateDIDOperation,
  KeyUsage,
  PublicKey,
  SignedAtalaOperation
}
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.services.NodeClientService

import _root_.java.io.BufferedInputStream

object E2ETestUtils {
  implicit val ecTrait = EC

  def addConnectionFromTokenRequest(token: String, clientKey: ECKeyPair): AddConnectionFromTokenRequest = {
    AddConnectionFromTokenRequest(
      token = token,
      holderEncodedPublicKey = Some(EncodedPublicKey(ByteString.copyFrom(clientKey.getPublicKey.getEncoded)))
    )
  }

  def createDid(
      masterKey: ECKeyPair,
      masterKeyId: String,
      issuanceKey: ECKeyPair,
      issuanceKeyId: String
  ): RegisterDIDRequest = {
    val createDidOp = CreateDIDOperation(
      didData = Some(
        node_models.CreateDIDOperation.DIDCreationData(
          publicKeys = Seq(
            PublicKey(
              id = masterKeyId,
              usage = KeyUsage.MASTER_KEY,
              keyData = PublicKey.KeyData.EcKeyData(
                NodeClientService.toTimestampInfoProto(masterKey.getPublicKey)
              )
            ),
            PublicKey(
              id = issuanceKeyId,
              usage = KeyUsage.ISSUING_KEY,
              keyData = PublicKey.KeyData.EcKeyData(
                NodeClientService.toTimestampInfoProto(issuanceKey.getPublicKey)
              )
            )
          )
        )
      )
    )

    val atalaOperation = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))

    val signedAtalaOperation = SignedAtalaOperation(
      signedWith = masterKeyId,
      operation = Some(atalaOperation),
      signature = ByteString.copyFrom(EC.signBytes(atalaOperation.toByteArray, masterKey.getPrivateKey).getData)
    )

    RegisterDIDRequest()
      .withCreateDidOperation(signedAtalaOperation)
      .withLogo(ByteString.EMPTY)
      .withName("mirror")
      .withRole(RegisterDIDRequest.Role.issuer)
  }

  def createAuthConfig(
      did: DID,
      masterKeys: ECKeyPair,
      masterKeyId: String,
      issuingKeys: ECKeyPair,
      issuingKeyId: String
  ): DidBasedAuthConfig = {
    DidBasedAuthConfig(
      did = did,
      didMasterKeyId = masterKeyId,
      didMasterKeyPair = masterKeys,
      didIssuingKeyId = issuingKeyId,
      didIssuingKeyPair = issuingKeys
    )
  }

  def readFileFromResource(name: String): Array[Byte] = {
    val stream = new BufferedInputStream(Thread.currentThread().getContextClassLoader.getResourceAsStream(name))
    val file =
      try LazyList.continually(stream.read).takeWhile(_ != -1).map(_.toByte).toArray
      finally stream.close()
    file
  }

}
