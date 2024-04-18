package io.iohk.atala.prism.node.repositories.daos

import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.implicits.legacy.instant._
import cats.syntax.functor._
import io.iohk.atala.prism.node.crypto.CryptoUtils.Sha256Hash
import io.iohk.atala.prism.node.models.DidSuffix
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.utils.syntax._

object DIDDataDAO {
  def insert(
      didSuffix: DidSuffix,
      lastOperation: Sha256Hash,
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val publishedOn = ledgerData.timestampInfo
    sql"""
         |INSERT INTO did_data (did_suffix, last_operation, published_on, published_on_absn, published_on_osn, transaction_id, ledger)
         |VALUES ($didSuffix, $lastOperation, ${publishedOn.getAtalaBlockTimestamp.toInstant},
         |  ${publishedOn.getAtalaBlockSequenceNumber}, ${publishedOn.getOperationSequenceNumber},
         |  ${ledgerData.transactionId}, ${ledgerData.ledger})
       """.stripMargin.update.run.void
  }

  def findByDidSuffix(didSuffix: DidSuffix): ConnectionIO[Option[DidSuffix]] = {
    sql"""
         |SELECT 1 FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].option.map(_.as(didSuffix))
  }

  def getLastOperation(
      didSuffix: DidSuffix
  ): ConnectionIO[Option[Sha256Hash]] = {
    sql"""
         |SELECT last_operation FROM did_data
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Sha256Hash].option
  }

  def updateLastOperation(
      didSuffix: DidSuffix,
      newLastOperation: Sha256Hash
  ): ConnectionIO[Int] = {
    sql"""
         |UPDATE did_data
         |SET last_operation = $newLastOperation
         |WHERE did_suffix = $didSuffix
       """.stripMargin.update.run
  }

  def all(): ConnectionIO[Seq[DidSuffix]] = {
    sql"""SELECT did_suffix FROM did_data"""
      .query[DidSuffix]
      .to[Seq]
  }
}
