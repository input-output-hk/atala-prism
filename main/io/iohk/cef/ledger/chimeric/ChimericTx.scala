package io.iohk.cef.ledger.chimeric

import io.iohk.cef.ledger.chimeric.errors._
import io.iohk.crypto._
import io.iohk.codecs.nio.auto._
import io.iohk.cef.ledger.Transaction
import io.iohk.cef.ledger.chimeric.ChimericLedgerState.getUtxoPartitionId
import io.iohk.cef.ledger.chimeric.errors._

case class ChimericTx(fragments: Seq[ChimericTxFragment]) extends Transaction[ChimericStateResult] {

  override def toString(): ChimericTxId = fragments.toString

  override def apply(currentState: ChimericLedgerState): ChimericStateOrError = {
    fragments.zipWithIndex
      .foldLeft[ChimericStateOrError](testSignatures(testPreservationOfValue(Right(currentState))))(
        (stateEither, current) => {
          stateEither.flatMap(state => {
            val (fragment, index) = current
            fragment(state, index, txId)
          })
        }
      )
  }

  override val partitionIds: Set[String] = {
    fragments.zipWithIndex.map { case (fragment, index) => fragment.partitionIds(txId, index) }.toSet.flatten
  }

  val txId: ChimericTxId = hash(fragments).toCompactString().replace(" ", "")

  private def testSignatures(currentStateEither: ChimericStateOrError): ChimericStateOrError =
    currentStateEither.flatMap { currentState =>
      val signatureFragments: Seq[SignatureTxFragment] = fragments.collect { case s: SignatureTxFragment => s }
      val signedFragments: Seq[ChimericTxFragment] = fragments.filterNot(_.isInstanceOf[SignatureTxFragment])
      val signingKeys: Seq[SigningPublicKey] = fragments.collect {
        case input: Input =>
          extractSigningKey(currentState, input)
        case withdrawal: Withdrawal => Some(withdrawal.address)
      }.flatten

      if (signatureFragments.length == signingKeys.length)
        testSignaturesCorrectness(currentState, signedFragments, signatureFragments, signingKeys)
      else
        Left(MissingSignature)
    }

  private def extractSigningKey(currentState: ChimericLedgerState, input: Input): Option[SigningPublicKey] = {

    val txOutQuery = getUtxoPartitionId(input.txOutRef)

    val maybeUtxoResult: Option[UtxoResult] =
      currentState.get(txOutQuery).collect { case r: UtxoResult => r }

    maybeUtxoResult.map(_.signingPublicKey)
  }

  private def testSignaturesCorrectness(
      currentState: ChimericLedgerState,
      fragments: Seq[ChimericTxFragment],
      signatureFragments: Seq[SignatureTxFragment],
      signingKeys: Seq[SigningPublicKey]
  ): ChimericStateOrError = {

    signatureFragments
      .zip(signingKeys)
      .foldLeft[ChimericStateOrError](Right(currentState))((stateEither, is) => {
        val (signatureFragment, signingPublicKey) = is
        stateEither.flatMap(state => testSignatureCorrectness(state, fragments, signatureFragment, signingPublicKey))
      })
  }

  private def testSignatureCorrectness(
      currentState: ChimericLedgerState,
      fragments: Seq[ChimericTxFragment],
      signatureFragment: SignatureTxFragment,
      signingPublicKey: SigningPublicKey
  ): ChimericStateOrError = {
    import io.iohk.codecs.nio.auto._

    if (isValidSignature(fragments, signatureFragment.signature, signingPublicKey))
      Right(currentState)
    else
      Left(InvalidSignature)
  }

  private def testPreservationOfValue(currentStateEither: ChimericStateOrError): ChimericStateOrError =
    currentStateEither.flatMap { currentState =>
      val totalValue = fragments.foldLeft(Value.Zero)(
        (sum, current) =>
          current match {
            case input: TxInputFragment => sum + input.value
            case output: TxOutputFragment => sum - output.value
            case _ => sum
          }
      )
      if (totalValue == Value.Zero) {
        Right(currentState)
      } else {
        Left(ValueNotPreserved(totalValue, fragments))
      }
    }
}
