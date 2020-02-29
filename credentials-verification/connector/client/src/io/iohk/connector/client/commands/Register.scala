package io.iohk.connector.client.commands

import java.security.{PublicKey => JPublicKey}

import com.google.protobuf.ByteString
import io.iohk.connector.client.Config
import io.iohk.cvp.connector.protos.RegisterDIDRequest.Role
import io.iohk.cvp.connector.protos.{ConnectorServiceGrpc, RegisterDIDRequest}
import io.iohk.cvp.crypto.ECKeys
import io.iohk.cvp.node_ops._

case class Register(
    keysToGenerate: Vector[(String, KeyUsage)] = Vector("master" -> KeyUsage.MASTER_KEY)
) extends Command {
  import Command.signOperation

  private def protoECKeyFromPublicKey(key: JPublicKey) = {
    val point = ECKeys.getECPoint(key)

    ECKeyData(
      curve = ECKeys.CURVE_NAME,
      x = ByteString.copyFrom(point.getAffineX.toByteArray),
      y = ByteString.copyFrom(point.getAffineY.toByteArray)
    )
  }

  override def run(api: ConnectorServiceGrpc.ConnectorServiceBlockingStub, config: Config): Unit = {
    val generatedKeys = keysToGenerate.map {
      case (keyId, usage) =>
        val keyPair = ECKeys.generateKeyPair()
        (keyId, usage, keyPair.getPrivate, keyPair.getPublic)
    }

    val publicKeys = generatedKeys.map {
      case (keyId, keyUsage, _, key) =>
        PublicKey(
          id = keyId,
          usage = keyUsage,
          keyData = PublicKey.KeyData.EcKeyData(protoECKeyFromPublicKey(key))
        )
    }

    val createDidOp = CreateDIDOperation(
      didData = Some(
        DIDData(
          publicKeys = publicKeys
        )
      )
    )

    val (masterKeyId, _, masterKey, _) = generatedKeys
      .find(_._2 == KeyUsage.MASTER_KEY)
      .getOrElse(throw new RuntimeException("The master key must be provided"))

    val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))
    val signedAtalaOp = signOperation(atalaOp, masterKeyId, masterKey)

    val request = RegisterDIDRequest()
      .withCreateDIDOperation(signedAtalaOp)
      .withLogo(ByteString.EMPTY)
      .withName("iohk-test")
      .withRole(Role.issuer)
    val response = api.registerDID(request)
    println(s"Created did with didSuffix: ${response.did}")
  }
}
