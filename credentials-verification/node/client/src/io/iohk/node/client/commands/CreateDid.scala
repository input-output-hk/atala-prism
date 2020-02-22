package io.iohk.node.client.commands

import io.iohk.cvp.crypto.ECKeys
import io.iohk.node.client.{Config, ProtoUtils, State, StateStorage}
import io.iohk.node.geud_node._
import io.iohk.node.models.SHA256Digest
import monocle.Optional
import monocle.macros.{GenLens, GenPrism}
import monocle.std.option.some
import scopt.OParser

case class CreateDid(
    keysToGenerate: Vector[(String, KeyUsage)] = Vector.empty,
    overwrite: Boolean = false,
    recreate: Boolean = false
) extends Command {
  import Command.signOperation

  override def run(api: NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val state = if (config.stateStorage.exists()) {
      if (recreate) {
        if (keysToGenerate.nonEmpty && !overwrite) {
          throw new IllegalStateException("Recreating DID with adding new keys requires overwrite flag")
        } else {
          StateStorage.load(config.stateStorage)
        }
      } else if (overwrite) {
        State()
      } else {
        throw new IllegalStateException("State storage exists; enable recreate or overwrite flag")
      }
    } else {
      if (recreate) {
        println("Not recreating: no state found")
      }
      State()
    }
    val generatedKeys = keysToGenerate.map {
      case (keyId, usage) =>
        val keyPair = ECKeys.generateKeyPair()
        (keyId, usage, keyPair.getPrivate, keyPair.getPublic)
    }

    val keys = generatedKeys ++ state.keys

    val publicKeys = keys.map {
      case (keyId, keyUsage, _, key) =>
        PublicKey(
          id = keyId,
          usage = keyUsage,
          keyData = PublicKey.KeyData.EcKeyData(ProtoUtils.protoECKeyFromPublicKey(key))
        )
    }

    val createDidOp = CreateDIDOperation(
      didData = Some(
        DIDData(
          publicKeys = publicKeys
        )
      )
    )

    val (masterKeyId, _, masterKey, _) = keys.find(_._2 == KeyUsage.MASTER_KEY).get

    val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.CreateDid(createDidOp))
    val signedAtalaOp = signOperation(atalaOp, masterKeyId, masterKey)
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)

    val response = api.createDID(signedAtalaOp)
    val didSuffix = response.id
    println(response.toProtoString)

    println(s"Created did with didSuffix: ${response.id}")

    println(s"Storing state to ${config.stateStorage}")
    StateStorage.save(
      config.stateStorage,
      state.copy(
        keys = keys.toList,
        lastOperationPerId = state.lastOperationPerId.+(response.id -> operationHash),
        didSuffix = Some(response.id)
      )
    )
  }
}

object CreateDid {
  import Config._
  val lens: Optional[Config, CreateDid] =
    GenLens[Config](_.command).composePrism(some).composePrism(GenPrism[Command, CreateDid])
  val keysLens = lens.composeLens(GenLens[CreateDid](_.keysToGenerate))

  def keyAppend[T](f: T => (String, KeyUsage)): (T, Config) => Config = { (x, c) =>
    keysLens.modify(v => v :+ f(x))(c)
  }

  val parser = {
    import parserBuilder._

    OParser.sequence(
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
      opt[Unit]("overwrite")
        .action(lens.composeLens(GenLens[CreateDid](_.overwrite)).optify(_ => true)),
      opt[Unit]("recreate")
        .action(lens.composeLens(GenLens[CreateDid](_.recreate)).optify(_ => true))
    )
  }
}
