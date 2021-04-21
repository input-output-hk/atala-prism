package io.iohk.atala.prism.models

import io.iohk.atala.prism.protos.common_models
import io.iohk.atala.prism.utils.syntax._

object ProtoCodecs {
  def toTransactionInfo(transactionInfo: TransactionInfo): common_models.TransactionInfo = {
    common_models
      .TransactionInfo(
        transactionId = transactionInfo.transactionId.toString,
        ledger = toLedger(transactionInfo.ledger),
        block = transactionInfo.block.map(toBlockInfo)
      )
  }

  def fromTransactionInfo(transactionInfo: common_models.TransactionInfo): TransactionInfo = {
    TransactionInfo(
      transactionId = TransactionId
        .from(transactionInfo.transactionId)
        .getOrElse(throw new RuntimeException("Corrupted transaction ID")),
      ledger = fromLedger(transactionInfo.ledger),
      block = transactionInfo.block.map(fromBlockInfo)
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

  def toBlockInfo(blockInfo: BlockInfo): common_models.BlockInfo = {
    common_models
      .BlockInfo()
      .withNumber(blockInfo.number)
      .withTimestamp(blockInfo.timestamp.toProtoTimestamp)
      .withIndex(blockInfo.index)
  }

  def fromBlockInfo(blockInfo: common_models.BlockInfo): BlockInfo = {
    BlockInfo(
      number = blockInfo.number,
      timestamp = blockInfo.timestamp.getOrElse(throw new RuntimeException("Missing timestamp")).toInstant,
      index = blockInfo.index
    )
  }
}
