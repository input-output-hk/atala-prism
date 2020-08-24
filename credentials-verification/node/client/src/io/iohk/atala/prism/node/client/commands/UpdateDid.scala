package io.iohk.atala.prism.node.client.commands

import com.google.protobuf.ByteString
import io.iohk.atala.crypto.EC
import io.iohk.cvp.crypto.SHA256Digest
import io.iohk.atala.prism.node.client.{Config, ProtoUtils, StateStorage}
import io.iohk.prism.protos.{node_api, node_models}
import monocle.Optional
import monocle.macros.{GenLens, GenPrism}
import monocle.std.option.some
import scopt.OParser

case class UpdateDid(
    keysToGenerate: Vector[(String, node_models.KeyUsage)] = Vector.empty,
    keysToRemove: Vector[String] = Vector.empty,
    didSuffix: Option[String] = None,
    previousOperation: Option[SHA256Digest] = None
) extends Command {

  import Command.signOperation

  override def run(api: node_api.NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {

    val state = StateStorage.load(config.stateStorage)
    val keys = state.keys

    val suffix = didSuffix.orElse(state.didSuffix).getOrElse {
      throw new IllegalStateException("No default did suffix value available, provide one via --did option")
    }

    val lastOperation = previousOperation
      .orElse(state.lastOperationPerId.get(suffix))
      .getOrElse(throw new IllegalStateException("Unknown did, please provide last operation hash manually"))

    val generatedKeys = keysToGenerate.map {
      case (keyId, usage) =>
        val keyPair = EC.generateKeyPair()
        (keyId, usage, keyPair.privateKey, keyPair.publicKey)
    }

    val addActions = generatedKeys.map {
      case (keyId, keyUsage, _, key) =>
        val publicKey = node_models.PublicKey(
          id = keyId,
          usage = keyUsage,
          keyData = node_models.PublicKey.KeyData.EcKeyData(ProtoUtils.protoECKeyFromPublicKey(key))
        )
        node_models.UpdateDIDAction(
          action = node_models.UpdateDIDAction.Action.AddKey(node_models.AddKeyAction(key = Some(publicKey)))
        )
    }

    val removeActions = keysToRemove.map { keyId =>
      node_models.UpdateDIDAction(
        action = node_models.UpdateDIDAction.Action.RemoveKey(node_models.RemoveKeyAction(keyId = keyId))
      )
    }

    val updateDidOp = node_models.UpdateDIDOperation(
      id = suffix,
      actions = addActions ++ removeActions,
      previousOperationHash = ByteString.copyFrom(lastOperation.value)
    )

    val (masterKeyId, _, masterKey, _) = keys.find(_._2 == node_models.KeyUsage.MASTER_KEY).get

    val atalaOp = node_models.AtalaOperation(operation = node_models.AtalaOperation.Operation.UpdateDid(updateDidOp))
    val signedAtalaOp = signOperation(atalaOp, masterKeyId, masterKey)
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)

    val response = api.updateDID(node_api.UpdateDIDRequest().withSignedOperation(signedAtalaOp))
    println(response.toProtoString)

    println(s"Update did with didSuffix: $suffix")

    println(s"Storing state to ${config.stateStorage}")

    val updatedKeys = keys.filterNot(key => keysToRemove.contains(key._1)) ++ generatedKeys

    StateStorage.save(
      config.stateStorage,
      state.copy(
        keys = updatedKeys,
        lastOperationPerId = state.lastOperationPerId.+(suffix -> operationHash)
      )
    )

  }
}

object UpdateDid {

  import Config._
  val lens: Optional[Config, UpdateDid] =
    GenLens[Config](_.command).composePrism(some).composePrism(GenPrism[Command, UpdateDid])
  private val keysLens = lens.composeLens(GenLens[UpdateDid](_.keysToGenerate))
  private val keyRemovalLens = lens.composeLens(GenLens[UpdateDid](_.keysToRemove))

  def keyAppend[T](f: T => (String, node_models.KeyUsage)): (T, Config) => Config = { (x, c) =>
    keysLens.modify(v => v :+ f(x))(c)
  }

  val parser: OParser[String, Config] = {
    import parserBuilder._

    OParser.sequence(
      opt[String]("did-suffix")
        .action(lens.composeLens(GenLens[UpdateDid](_.didSuffix)).optify(Some(_))),
      opt[String]("previous-operation")
        .valueName("<hash>")
        .action(
          lens.composeLens(GenLens[UpdateDid](_.previousOperation)).optify(s => Some(SHA256Digest.fromHex(s)))
        ),
      opt[String]("generate-master-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, node_models.KeyUsage.MASTER_KEY))),
      opt[String]("generate-issuing-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, node_models.KeyUsage.ISSUING_KEY))),
      opt[String]("generate-communication-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, node_models.KeyUsage.COMMUNICATION_KEY))),
      opt[String]("generate-authentication-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, node_models.KeyUsage.AUTHENTICATION_KEY))),
      opt[String]("remove-key")
        .valueName("<key-id>")
        .unbounded()
        .action((x, c) => keyRemovalLens.modify(v => v :+ x)(c))
    )
  }
}
