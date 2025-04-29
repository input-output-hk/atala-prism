package io.iohk.atala.prism.node.cardano.dbsync.repositories

import doobie.Get
import doobie.Read
import doobie.implicits.legacy.instant._
import io.circe.Json
import io.iohk.atala.prism.node.cardano.models.BlockHash
import io.iohk.atala.prism.node.cardano.models.BlockHeader
import io.iohk.atala.prism.node.cardano.models.Transaction
import io.iohk.atala.prism.node.cardano.models.TransactionMetadata
import io.iohk.atala.prism.node.models.TransactionId
import io.iohk.atala.prism.node.repositories.daos.BaseDAO

import java.time.Instant

package object daos extends BaseDAO {
  case class BlockTransactionData(
      blockHash: BlockHash,
      blockNo: Int,
      time: Instant,
      id: TransactionId,
      blockIndex: Int,
      metadata: Option[TransactionMetadata]
  )

  private[daos] implicit val blockHashGet: Get[BlockHash] =
    Get[Array[Byte]].tmap { bytes =>
      BlockHash
        .from(bytes)
        .getOrElse(throw new RuntimeException("Corrupted block hash"))
    }

  private[daos] implicit val blockHeaderRead: Read[BlockHeader] = {
    Read[(BlockHash, Int, Instant, Option[BlockHash])]
      .map((BlockHeader.apply _).tupled)
  }

  private[daos] implicit val blockTransactionRead: Read[BlockTransactionData] = {
    Read[(BlockHash, Int, Instant, TransactionId, Int, Option[Int], Option[String])]
      .map {
        case (
              blockHash,
              blockNo,
              time,
              transactionId,
              blockIndex,
              metadataKey,
              metadataJson
            ) =>
          BlockTransactionData(
            blockHash,
            blockNo,
            time,
            transactionId,
            blockIndex,
            // Merge `metadataKey` and `metadataJson` columns into `TransactionMetadata`
            metadataKey.map(key => {
              val parsedMetadataJson = io.circe.parser
                .parse(metadataJson.getOrElse("{}"))
                .getOrElse(
                  throw new RuntimeException(
                    s"Metadata of transaction $transactionId could not be parsed"
                  )
                )

              TransactionMetadata(Json.obj(key.toString -> parsedMetadataJson))
            })
          )
      }
  }
  private[daos] implicit val transactionRead: Read[Transaction] = {
    Read[(TransactionId, BlockHash, Int, Option[Int], Option[String])]
      .map {
        case (
              transactionId,
              blockHash,
              blockIndex,
              metadataKey,
              metadataJson
            ) =>
          Transaction(
            transactionId,
            blockHash,
            blockIndex,
            // Merge `metadataKey` and `metadataJson` columns into `TransactionMetadata`
            metadataKey.map(key => {
              val parsedMetadataJson = io.circe.parser
                .parse(metadataJson.getOrElse("{}"))
                .getOrElse(
                  throw new RuntimeException(
                    s"Metadata of transaction $transactionId could not be parsed"
                  )
                )

              TransactionMetadata(Json.obj(key.toString -> parsedMetadataJson))
            })
          )
      }
  }

}
