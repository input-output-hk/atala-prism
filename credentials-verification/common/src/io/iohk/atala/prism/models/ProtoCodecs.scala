package io.iohk.atala.prism.models

import io.iohk.prism.protos.common_models

object ProtoCodecs {
  def toTransactionInfo(transactionInfo: TransactionInfo): common_models.TransactionInfo = {
    common_models.TransactionInfo().withId(transactionInfo.id.toString).withLedger(toLedger(transactionInfo.ledger))
  }

  def fromTransactionInfo(transactionInfo: common_models.TransactionInfo): TransactionInfo = {
    TransactionInfo(
      TransactionId
        .from(transactionInfo.id)
        .getOrElse(throw new RuntimeException("Corrupted transaction ID")),
      fromLedger(transactionInfo.ledger)
    )
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

  def fromLedger(ledger: common_models.Ledger): Ledger = {
    ledger match {
      case common_models.Ledger.IN_MEMORY => Ledger.InMemory
      case common_models.Ledger.BITCOIN_TESTNET => Ledger.BitcoinTestnet
      case common_models.Ledger.BITCOIN_MAINNET => Ledger.BitcoinMainnet
      case common_models.Ledger.CARDANO_TESTNET => Ledger.CardanoTestnet
      case common_models.Ledger.CARDANO_MAINNET => Ledger.CardanoMainnet
      case _ => throw new IllegalArgumentException(s"Unexpected ledger: $ledger")
    }
  }
}
