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

case class IssueCredential(issuer: Option[String] = None, contentHash: Option[SHA256Digest] = None) extends Command {
  override def run(api: NodeServiceGrpc.NodeServiceBlockingStub, config: Config): Unit = {
    val state = StateStorage.load(config.stateStorage)
    val keys = state.keys

    val issuanceJavaDate = LocalDate.now()
    val issuanceDate = Date(issuanceJavaDate.getYear, issuanceJavaDate.getMonthValue, issuanceJavaDate.getDayOfMonth)

    val issuerVal = issuer
      .orElse(state.didSuffix)
      .getOrElse(
        throw new IllegalStateException("No default did suffix value available, provide one via --issuer option")
      )

    val issueCredentialOp = IssueCredentialOperation(
      credentialData = Some(
        CredentialData(
          issuer = issuerVal,
          contentHash = ByteString.copyFrom(contentHash.get.value),
          issuanceDate = Some(issuanceDate)
        )
      )
    )

    val atalaOp = AtalaOperation(operation = AtalaOperation.Operation.IssueCredential(issueCredentialOp))
    val (issuingKeyId, _, issuingKey, _) = keys.find(_._2 == KeyUsage.ISSUING_KEY).get
    val signedAtalaOp = Command.signOperation(atalaOp, issuingKeyId, issuingKey)
    val operationHash = SHA256Digest.compute(atalaOp.toByteArray)

    val response = api.issueCredential(signedAtalaOp)
    println(response.toProtoString)

    println(s"Created credential with id: ${response.id}")

    println(s"Storing state to ${config.stateStorage}")
    StateStorage.save(
      config.stateStorage,
      state.copy(lastOperationPerId = state.lastOperationPerId.+(response.id -> operationHash))
    )
  }
}

object IssueCredential {
  import Config._
  val lens: Optional[Config, IssueCredential] =
    GenLens[Config](_.command).composePrism(some).composePrism(GenPrism[Command, IssueCredential])

  val parser = {
    import parserBuilder._

    OParser.sequence(
      opt[String]("issuer")
        .action(lens.composeLens(GenLens[IssueCredential](_.issuer)).optify(Some(_))),
      opt[String]("content")
        .action(
          lens
            .composeLens(GenLens[IssueCredential](_.contentHash))
            .optify((s: String) => Some(SHA256Digest.compute(s.getBytes())))
        ),
      opt[String]("content-hash")
        .action(
          lens
            .composeLens(GenLens[IssueCredential](_.contentHash))
            .optify((s: String) => Some(SHA256Digest.fromHex(s)))
        )
    )
  }
}
