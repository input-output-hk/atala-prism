package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import cats.syntax.functor._
import io.iohk.atala.prism.crypto.SHA256Digest
import io.iohk.atala.prism.identity.DIDSuffix
import io.iohk.atala.prism.node.models.nodeState.LedgerData

object DIDDataDAO {
  def insert(
      didSuffix: DIDSuffix,
      lastOperation: SHA256Digest,
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val publishedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO did_data (did_suffix, last_operation, published_on, published_on_absn, published_on_osn, transaction_id, ledger)
         |VALUES ($didSuffix, $lastOperation, ${publishedOn.atalaBlockTimestamp},
         |  ${publishedOn.atalaBlockSequenceNumber}, ${publishedOn.operationSequenceNumber},
         |  ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.update.run.void
  }

  def findByDidSuffix(didSuffix: DIDSuffix): ConnectionIO[Option[DIDSuffix]] = {
    sql"""
         |SELECT 1 FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].option.map(_.as(didSuffix))
  }

  def getLastOperation(didSuffix: DIDSuffix): ConnectionIO[Option[SHA256Digest]] = {
    sql"""
         |SELECT last_operation FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[SHA256Digest].option
  }

  def updateLastOperation(didSuffix: DIDSuffix, newLastOperation: SHA256Digest): ConnectionIO[Int] = {
    sql"""
         |UPDATE did_data
         |SET last_operation = $newLastOperation
         |WHERE did_suffix = $didSuffix
       """.stripMargin.update.run
  }

  def all(): ConnectionIO[Seq[DIDSuffix]] = {
    sql"""SELECT did_suffix FROM did_data"""
      .query[DIDSuffix]
      .to[Seq]
  }
}
