package io.iohk.node.client.commands

import com.google.protobuf.ByteString
import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.client.{Config, ProtoUtils, StateStorage}
import io.iohk.node.geud_node._
import io.iohk.node.models.SHA256Digest
import monocle.Optional
import monocle.macros.{GenLens, GenPrism}
import monocle.std.option.some
import scopt.OParser

case class UpdateDid(
    keysToGenerate: Vector[(String, KeyUsage)] = Vector.empty,
    keysToRemove: Vector[String] = Vector.empty,
    didSuffix: Option[String] = None,
    previousOperation: Option[SHA256Digest] = None
) extends Command {

  import Command.signOperation

  override def run(api: NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {

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
        val keyPair = ECKeys.generateKeyPair()
        (keyId, usage, keyPair.getPrivate, keyPair.getPublic)
    }

    val addActions = generatedKeys.map {
      case (keyId, keyUsage, _, key) =>
        val publicKey = PublicKey(
          id = keyId,
          usage = keyUsage,
          keyData = PublicKey.KeyData.EcKeyData(ProtoUtils.protoECKeyFromPublicKey(key))
        )
        UpdateDIDAction(action = UpdateDIDAction.Action.AddKey(AddKeyAction(key = Some(publicKey))))
    }

    val removeActions = keysToRemove.map { keyId =>
      UpdateDIDAction(action = UpdateDIDAction.Action.RemoveKey(RemoveKeyAction(keyId = keyId)))
    }

    val updateDidOp = UpdateDIDOperation(
      id = suffix,
      actions = addActions ++ removeActions,
      previousOperationHash = ByteString.copyFrom(lastOperation.value)
    )

    val (masterKeyId, _, masterKey, _) = keys.find(_._2 == KeyUsage.MASTER_KEY).get

    val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.UpdateDid(updateDidOp))
    val signedAtalaOp = signOperation(atalaOp, masterKeyId, masterKey)
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)

    val response = api.updateDID(signedAtalaOp)
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

  def keyAppend[T](f: T => (String, KeyUsage)): (T, Config) => Config = { (x, c) =>
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
        .action(keyAppend(name => (name, KeyUsage.MASTER_KEY))),
      opt[String]("generate-issuing-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, KeyUsage.ISSUING_KEY))),
      opt[String]("generate-communication-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, KeyUsage.COMMUNICATION_KEY))),
      opt[String]("generate-authentication-key")
        .valueName("<key-id>")
        .unbounded()
        .action(keyAppend(name => (name, KeyUsage.AUTHENTICATION_KEY))),
      opt[String]("remove-key")
        .valueName("<key-id>")
        .unbounded()
        .action((x, c) => keyRemovalLens.modify(v => v :+ x)(c))
    )
  }
}
