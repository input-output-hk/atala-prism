package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import cats.syntax.functor._
import io.iohk.atala.prism.kotlin.crypto.Sha256Digest
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.utils.syntax._

object DIDDataDAO {
  def insert(
      didSuffix: String,
      lastOperation: Sha256Digest,
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val publishedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO did_data (did_suffix, last_operation, published_on, published_on_absn, published_on_osn, transaction_id, ledger)
         |VALUES ($didSuffix, $lastOperation, ${publishedOn.getBlockTimestamp.toInstant},
         |  ${publishedOn.getBlockSequenceNumber}, ${publishedOn.getOperationSequenceNumber},
         |  ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.update.run.void
  }

  def findByDidSuffix(didSuffix: String): ConnectionIO[Option[String]] = {
    sql"""
         |SELECT 1 FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].option.map(_.as(didSuffix))
  }

  def getLastOperation(didSuffix: String): ConnectionIO[Option[Sha256Digest]] = {
    sql"""
         |SELECT last_operation FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Sha256Digest].option
  }

  def updateLastOperation(didSuffix: String, newLastOperation: Sha256Digest): ConnectionIO[Int] = {
    sql"""
         |UPDATE did_data
         |SET last_operation = $newLastOperation
         |WHERE did_suffix = $didSuffix
       """.stripMargin.update.run
  }

  def all(): ConnectionIO[Seq[String]] = {
    sql"""SELECT did_suffix FROM did_data"""
      .query[String]
      .to[Seq]
  }
}
