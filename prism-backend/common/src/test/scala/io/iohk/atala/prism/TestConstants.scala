package io.iohk.atala.prism

import io.iohk.atala.prism.models.TransactionId

object TestConstants {

  val testTxId: TransactionId =
    TransactionId
      .from("3d488d9381b09954b5a9606b365ab0aaeca6aa750bdba79436e416ad6702226a")
      .get

}
