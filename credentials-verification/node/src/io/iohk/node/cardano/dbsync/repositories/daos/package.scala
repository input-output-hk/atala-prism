package io.iohk.node.cardano.dbsync.repositories

import java.time.Instant

import doobie.util.{Get, Read}
import io.iohk.node.cardano.models.{BlockHash, BlockHeader, Transaction, TransactionId}

package object daos {
  private[daos] implicit val blockHashGet: Get[BlockHash] = Get[Array[Byte]].tmap { bytes =>
    BlockHash
      .fromBytesBE(bytes)
      .getOrElse(throw new RuntimeException("Corrupted block hash"))
  }

  private[daos] implicit val blockHeaderRead: Read[BlockHeader] = {
    Read[(BlockHash, Int, Instant, Option[BlockHash])]
      .map((BlockHeader.apply _).tupled)
  }

  private[daos] implicit val transactionIdGet: Get[TransactionId] = Get[Array[Byte]].tmap { bytes =>
    TransactionId
      .fromBytesBE(bytes)
      .getOrElse(throw new RuntimeException("Corrupted transaction ID"))
  }

  private[daos] implicit val transactionRead: Read[Transaction] = {
    Read[(TransactionId, BlockHash)]
      .map((Transaction.apply _).tupled)
  }
}
