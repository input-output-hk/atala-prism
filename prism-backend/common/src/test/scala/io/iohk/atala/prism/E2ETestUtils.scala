package io.iohk.atala.prism

import java.io.BufferedInputStream

import com.google.protobuf.ByteString
import io.iohk.atala.prism.crypto.{EC, ECKeyPair}
import io.iohk.atala.prism.identity.DID
import io.iohk.atala.prism.protos.connector_api.{AddConnectionFromTokenRequest, RegisterDIDRequest}
import io.iohk.atala.prism.protos.connector_models.EncodedPublicKey
import io.iohk.atala.prism.protos.node_models.{
  AtalaOperation,
  CreateDIDOperation,
  DIDData,
  KeyUsage,
  PublicKey,
  SignedAtalaOperation
}
import io.iohk.atala.prism.services.BaseGrpcClientService.DidBasedAuthConfig
import io.iohk.atala.prism.services.NodeClientService

object E2ETestUtils {
  implicit val ecTrait = EC

  def addConnectionFromTokenRequest(token: String, clientKey: ECKeyPair): AddConnectionFromTokenRequest = {
    AddConnectionFromTokenRequest(
      token = token,
      holderEncodedPublicKey = Some(EncodedPublicKey(ByteString.copyFrom(clientKey.publicKey.getEncoded)))
    )
  }

  def createDid(keys: ECKeyPair, keyId: String, clientKey: ECKeyPair): RegisterDIDRequest = {
    val createDidOp = CreateDIDOperation(
      didData = Some(
        DIDData(
          publicKeys = Seq(
            PublicKey(
              id = keyId,
              usage = KeyUsage.MASTER_KEY,
              keyData = PublicKey.KeyData.EcKeyData(
                NodeClientService.toTimestampInfoProto(keys.publicKey)
              )
            )
          )
        )
      )
    )

    val atalaOperation = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))

    val signedAtalaOperation = SignedAtalaOperation(
      signedWith = keyId,
      operation = Some(atalaOperation),
      signature = ByteString.copyFrom(ecTrait.sign(atalaOperation.toByteArray, clientKey.privateKey).data)
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
