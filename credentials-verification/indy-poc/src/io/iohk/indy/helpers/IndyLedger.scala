package io.iohk.indy.helpers

import io.iohk.indy.models._
import org.hyperledger.indy.sdk.ledger.Ledger

object IndyLedger {

  def buildNymRequest(submitter: Did, target: WalletDid, targetRole: UserRole): JsonString = {
    val result = Ledger
      .buildNymRequest(submitter.string, target.did.string, target.verificationKey, null, targetRole.string)
      .get
    JsonString(result)
  }
}
