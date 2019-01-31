package io.iohk.cef.frontend.models

import io.iohk.crypto._
import io.iohk.cef.ledger.LedgerId
import io.iohk.cef.ledger.chimeric._

sealed trait CreateChimericTransactionFragment {

  val fragment: ChimericTxFragment
}

case class CreateNonSignableChimericTransactionFragment(override val fragment: NonSignableChimericTxFragment)
    extends CreateChimericTransactionFragment

case class CreateSignableChimericTransactionFragment(
    override val fragment: SignableChimericTxFragment,
    signingPrivateKey: SigningPrivateKey
) extends CreateChimericTransactionFragment

case class CreateChimericTransactionRequest(fragments: Seq[CreateChimericTransactionFragment], ledgerId: LedgerId)

case class SubmitChimericTransactionFragment(fragment: ChimericTxFragment)

case class SubmitChimericTransactionRequest(fragments: Seq[SubmitChimericTransactionFragment], ledgerId: LedgerId)
