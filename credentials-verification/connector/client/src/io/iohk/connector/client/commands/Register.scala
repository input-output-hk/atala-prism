package io.iohk.atala.prism.connector.client.commands

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.{EC, ECConfig, ECPublicKey}
import io.iohk.atala.prism.connector.client.Config
import io.iohk.atala.prism.protos.{connector_api, node_models}

case class Register(
    keysToGenerate: Vector[(String, node_models.KeyUsage)] = Vector("master" -> node_models.KeyUsage.MASTER_KEY)
) extends Command {
  import Command.signOperation

  private def protoECKeyFromPublicKey(key: ECPublicKey) = {
    node_models.ECKeyData(
      curve = ECConfig.CURVE_NAME,
      x = ByteString.copyFrom(key.getCurvePoint.x.toByteArray),
      y = ByteString.copyFrom(key.getCurvePoint.y.toByteArray)
    )
  }

  override def run(api: connector_api.ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val generatedKeys = keysToGenerate.map {
      case (keyId, usage) =>
        val keyPair = EC.generateKeyPair()
        (keyId, usage, keyPair.privateKey, keyPair.publicKey)
    }

    val publicKeys = generatedKeys.map {
      case (keyId, keyUsage, _, key) =>
        node_models.PublicKey(
          id = keyId,
          usage = keyUsage,
          keyData = node_models.PublicKey.KeyData.EcKeyData(protoECKeyFromPublicKey(key))
        )
    }

    val createDidOp = node_models.CreateDIDOperation(
      didData = Some(
        node_models.DIDData(
          publicKeys = publicKeys
        )
      )
    )

    val (masterKeyId, _, masterKey, _) = generatedKeys
      .find(_._2 == node_models.KeyUsage.MASTER_KEY)
      .getOrElse(throw new RuntimeException("The master key must be provided"))

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.CreateDid(createDidOp))
    val signedAtalaOp = signOperation(atalaOp, masterKeyId, masterKey)

    val request = connector_api
      .RegisterDIDRequest()
      .withCreateDIDOperation(signedAtalaOp)
      .withLogo(ByteString.EMPTY)
      .withName("iohk-test")
      .withRole(connector_api.RegisterDIDRequest.Role.issuer)
    val response = api.registerDID(request)
    println(s"Created did with didSuffix: ${response.did}")
  }
}
