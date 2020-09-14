package io.iohk.atala.prism.models

import io.iohk.prism.protos.common_models

object ProtoCodecs {
  def toTransactionInfo(transactionInfo: TransactionInfo): common_models.TransactionInfo = {
    common_models.TransactionInfo().withId(transactionInfo.id.toString).withLedger(toLedger(transactionInfo.ledger))
  }

  def toLedger(ledger: Ledger): common_models.Ledger = {
    ledger match {
      case Ledger.InMemory => common_models.Ledger.IN_MEMORY
      case Ledger.BitcoinTestnet => common_models.Ledger.BITCOIN_TESTNET
      case Ledger.BitcoinMainnet => common_models.Ledger.BITCOIN_MAINNET
      case Ledger.CardanoTestnet => common_models.Ledger.CARDANO_TESTNET
      case Ledger.CardanoMainnet => common_models.Ledger.CARDANO_MAINNET
      case _ => throw new IllegalArgumentException(s"Unexpected ledger: $ledger")
    }
  }
}
