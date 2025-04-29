package io.iohk.atala.prism.node.repositories.daos

import cats.data.NonEmptyList
import cats.syntax.functor._
import doobie.Fragment
import doobie.Fragments.in
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.free.connection.unit
import doobie.implicits._
import doobie.implicits.legacy.instant._
import doobie.util.update.Update
import io.iohk.atala.prism.node.models._

import java.time.Instant

object AtalaObjectsDAO {

  case class AtalaObjectCreateData(
      objectId: AtalaObjectId,
      byteContent: Array[Byte],
      status: AtalaObjectStatus
  )
  case class AtalaObjectSetTransactionInfo(
      objectId: AtalaObjectId,
      transactionInfo: TransactionInfo
  )
  case class AtalaObjectCreateDataWithReceivedAt(
      objectId: AtalaObjectId,
      byteContent: Array[Byte],
      status: AtalaObjectStatus
  )

  def insertMany(objects: List[AtalaObjectCreateData]): ConnectionIO[Int] = {
    // Bulk insert objects
    Update[(AtalaObjectId, Array[Byte], AtalaObjectStatus, Instant)](
      """
      INSERT INTO atala_objects (atala_object_id, object_content, atala_object_status, received_at)
      VALUES (?, ?, ?, ?)
      ON CONFLICT (atala_object_id) DO NOTHING
      """
    ).updateMany(objects.map(d => (d.objectId, d.byteContent, d.status, Instant.now())))
  }

  def setManyTransactionInfo(
      data: List[AtalaObjectSetTransactionInfo]
  ): ConnectionIO[Int] = {
    // Extract valid transactions with blocks
    val validTransactions = for {
      item <- data
      block <- item.transactionInfo.block.toList
    } yield (
      item.objectId,
      item.transactionInfo.ledger,
      block.number,
      block.index,
      block.timestamp,
      item.transactionInfo.transactionId
    )

    // Log count before deduplication
    println(s"INITIAL COUNT: Total of ${validTransactions.size} records before deduplication")

    // Find duplicates by object ID
    val groupedByObjectId = validTransactions.groupBy(_._1)
    val duplicateGroups = groupedByObjectId.filter(_._2.size > 1)
    val uniqueGroups = groupedByObjectId.filter(_._2.size == 1)

    // Log detailed counts
    println(s"DEDUPLICATION STATS:")
    println(s"  Total input objects: ${data.size}")
    println(s"  Valid transactions with blocks: ${validTransactions.size}")
    println(s"  Unique object IDs: ${groupedByObjectId.size}")
    println(s"  Objects with duplicates: ${duplicateGroups.size}")
    println(s"  Objects without duplicates: ${uniqueGroups.size}")
    println(s"  Total duplicate records: ${validTransactions.size - groupedByObjectId.size}")

    // Log duplicates with full details
    duplicateGroups.foreach { case (objectId, dupes) =>
      println(s"DUPLICATE DETECTED for object ID: $objectId, found ${dupes.size} records:")
      dupes.zipWithIndex.foreach { case (dupe, index) =>
        println(s"  Duplicate #${index + 1}: $dupe")
      }
    }

    // Keep only one record per object ID (the first one)
    val dedupedTransactions = groupedByObjectId.map { case (_, txs) => txs.head }.toList

    // Log final count after deduplication
    println(s"FINAL COUNT: Total of ${dedupedTransactions.size} records after deduplication")
    println(s"REMOVED: ${validTransactions.size - dedupedTransactions.size} duplicate records")

    // Insert the unique transactions
    Update[(AtalaObjectId, Ledger, Int, Int, Instant, TransactionId)](
      """
      INSERT INTO atala_object_txs 
      (atala_object_id, ledger, block_number, block_index, block_timestamp, transaction_id)
      VALUES (?, ?, ?, ?, ?, ?)
      """
    ).updateMany(dedupedTransactions)
  }

  def insert(data: AtalaObjectCreateData): ConnectionIO[Int] = {
    sql"""
         |INSERT INTO atala_objects (atala_object_id, object_content, atala_object_status, received_at)
         |VALUES (${data.objectId}, ${data.byteContent}, ${data.status}, ${Instant
        .now()})
         |ON CONFLICT (atala_object_id) DO NOTHING
       """.stripMargin.update.run
  }

  def setTransactionInfo(
      data: AtalaObjectSetTransactionInfo
  ): ConnectionIO[Unit] = {
    val transaction = data.transactionInfo
    transaction.block match {
      case Some(block) =>
        sql"""
             |INSERT INTO atala_object_txs (atala_object_id, ledger, block_number, block_index, block_timestamp, transaction_id)
             |VALUES (${data.objectId}, ${transaction.ledger}, ${block.number}, ${block.index}, ${block.timestamp}, ${transaction.transactionId})
           """.stripMargin.update.run.void
      case _ =>
        connection.raiseError(
          new IllegalArgumentException("Transaction has bo block")
        )
    }
  }

  // Returning all transactions in ledger which correspond to an AtalaObject with the status Processed
  // Return sorted by block time in descending order
  def getConfirmedTransactions(
      lastSeenTransactionId: Option[TransactionId],
      limit: Option[Int]
  ): ConnectionIO[List[TransactionInfo]] = {
    val limitFr = limit.fold(Fragment.empty)(l => fr"LIMIT $l")
    val baseFr: Fragment = lastSeenTransactionId match {
      case None =>
        sql"""
             |SELECT tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
             |FROM atala_object_txs AS tx
             |INNER JOIN atala_objects AS obj ON tx.atala_object_id = obj.atala_object_id
             |WHERE obj.atala_object_status = 'PROCESSED'
             |ORDER BY tx.block_number DESC, tx.block_index DESC
       """.stripMargin
      case Some(lastSeenId) =>
        sql"""
            |WITH CTE AS (
            |  SELECT block_number AS last_seen_block_level, block_index AS last_seen_tx_index
            |  FROM atala_object_txs
            |  WHERE transaction_id = $lastSeenId
            |)
            |SELECT tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
            |FROM atala_object_txs AS tx
            |  INNER JOIN atala_objects AS obj ON tx.atala_object_id = obj.atala_object_id
            |  CROSS JOIN CTE
            |WHERE obj.atala_object_status = 'PROCESSED' AND
            |       (tx.block_number < last_seen_block_level OR
                          tx.block_number = last_seen_block_level AND tx.block_index < last_seen_tx_index)
            |ORDER BY tx.block_number DESC, tx.block_index DESC
         """.stripMargin
    }
    (baseFr ++ limitFr)
      .query[TransactionInfo]
      .to[List]
  }

  def get(objectId: AtalaObjectId): ConnectionIO[Option[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM atala_objects AS obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |WHERE obj.atala_object_id = $objectId
       """.stripMargin
      .query[AtalaObjectInfo]
      .option
  }
  // ??? Since we are syncing for first time this should be in processed status
  def getProcessedObjectInfos: ConnectionIO[List[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM
         |(
         |  SELECT *
         |  FROM atala_objects AS obj
         |  WHERE atala_object_status = 'PROCESSED'
         |) as obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |ORDER BY tx.block_number ASC, tx.block_index ASC;
       """.stripMargin
      .query[AtalaObjectInfo]
      .to[List]
  }

  def getNotPublishedObjectInfos: ConnectionIO[List[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM
         |(
         |  SELECT *
         |  FROM atala_objects AS obj
         |  WHERE atala_object_status = 'PENDING' and NOT EXISTS
         |  (
         |    SELECT 1
         |      FROM atala_object_tx_submissions
         |      WHERE atala_object_id = obj.atala_object_id AND status != 'DELETED'
         |  )
         |) as obj
         |  LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |ORDER BY obj.received_at ASC;
       """.stripMargin
      .query[AtalaObjectInfo]
      .to[List]
  }

  def getNotProcessedAtalaObjects: ConnectionIO[List[AtalaObjectInfo]] = {
    sql"""
         |SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
         |       tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
         |FROM atala_objects AS obj
         |LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
         |WHERE obj.atala_object_status = 'SCHEDULED' or obj.atala_object_status = 'PENDING'
         |ORDER BY obj.received_at ASC;
       """.stripMargin
      .query[AtalaObjectInfo]
      .to[List]
  }

  def updateObjectStatus(
      objectId: AtalaObjectId,
      newStatus: AtalaObjectStatus
  ): ConnectionIO[Unit] = {
    sql"""
         |UPDATE atala_objects
         |SET atala_object_status = $newStatus
         |WHERE atala_object_id = $objectId
      """.stripMargin.update.run.void
  }

  def updateObjectStatus(oldObjectStatus: AtalaObjectStatus, newObjectStatus: AtalaObjectStatus): ConnectionIO[Int] =
    sql"""
         |UPDATE atala_objects
         |SET atala_object_status = $newObjectStatus
         |WHERE atala_object_status = $oldObjectStatus
       """.stripMargin.update.run

  def updateObjectStatusBatch(
      objectIds: List[AtalaObjectId],
      newStatus: AtalaObjectStatus
  ): ConnectionIO[Unit] = {
    NonEmptyList.fromList(objectIds).fold(unit) { objectIdsNonEmpty =>
      val fragment = fr"UPDATE atala_objects" ++
        fr"SET atala_object_status = $newStatus" ++
        fr"WHERE" ++ in(fr"atala_object_id", objectIdsNonEmpty)
      fragment.update.run.void
    }
  }

  def getAtalaObjectsInfo(objectIds: List[AtalaObjectId]): ConnectionIO[List[AtalaObjectInfo]] = {
    NonEmptyList.fromList(objectIds).fold(connection.pure(List.empty[AtalaObjectInfo])) { objectIdsNonEmpty =>
      val q = fr"""
        SELECT obj.atala_object_id, obj.object_content, obj.atala_object_status,
               tx.transaction_id, tx.ledger, tx.block_number, tx.block_timestamp, tx.block_index
        FROM atala_objects AS obj
          LEFT OUTER JOIN atala_object_txs AS tx ON tx.atala_object_id = obj.atala_object_id
        WHERE """ ++ in(
        fr"obj.atala_object_id",
        objectIdsNonEmpty
      ) ++ fr" ORDER BY  tx.block_number ASC, tx.block_index ASC"

      q.query[AtalaObjectInfo]
        .to[List]
    }
  }
}
