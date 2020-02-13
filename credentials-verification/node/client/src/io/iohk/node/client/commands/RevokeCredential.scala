package io.iohk.node.client.commands

import java.time.LocalDate

import com.google.protobuf.ByteString
import io.iohk.node.client.{Config, State, StateStorage}
import io.iohk.node.models.SHA256Digest
import io.iohk.nodenew.geud_node_new._
import monocle.Optional
import monocle.macros.{GenLens, GenPrism}
import monocle.std.option.some
import scopt.OParser

case class RevokeCredential(credentialId: String = "", previousOperation: Option[SHA256Digest] = None) extends Command {
  override def run(api: NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val state = StateStorage.load(config.stateStorage)
    val keys = state.keys

    val lastOperation = previousOperation
      .orElse(state.lastOperationPerId.get(credentialId))
      .getOrElse(throw new IllegalStateException("Unknown credential, please provide last operation hash manually"))

    val revocationJavaDate = LocalDate.now()
    val revocationDate =
      Date(revocationJavaDate.getYear, revocationJavaDate.getMonthValue, revocationJavaDate.getDayOfMonth)

    val revokeCredentialOp = RevokeCredentialOperation(
      credentialId = credentialId,
      previousOperationHash = ByteString.copyFrom(lastOperation.value),
      revocationDate = Some(revocationDate)
    )

    val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.RevokeCredential(revokeCredentialOp))
    val (issuingKeyId, _, issuingKey, _) = keys.find(_._2 == KeyUsage.ISSUING_KEY).get
    val signedAtalaOp = Command.signOperation(atalaOp, issuingKeyId, issuingKey)
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)

    val response = api.revokeCredential(signedAtalaOp)
    println(response.toProtoString)

    println("Revoked credential")

    println(s"Storing state to ${config.stateStorage}")
    StateStorage.save(
      config.stateStorage,
      State(keys.toList, state.lastOperationPerId.+(credentialId -> operationHash))
    )
  }
}

object RevokeCredential {
  import Config._
  val lens: Optional[Config, RevokeCredential] =
    GenLens[Config](_.command).composePrism(some).composePrism(GenPrism[Command, RevokeCredential])

  val parser = {
    import parserBuilder._

    OParser.sequence(
      opt[String]("credential")
        .required()
        .valueName("<credential id>")
        .action(lens.composeLens(GenLens[RevokeCredential](_.credentialId)).optify),
      opt[String]("previous-operation")
        .valueName("<hash>")
        .action(
          lens.composeLens(GenLens[RevokeCredential](_.previousOperation)).optify(s => Some(SHA256Digest.fromHex(s)))
        )
    )
  }
}
