package io.iohk.atala.prism.node.repositories.daos

import cats.syntax.functor._
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import io.iohk.atala.prism.models.DidSuffix
import io.iohk.atala.prism.node.models.ProtocolVersion.InitialProtocolVersion
import io.iohk.atala.prism.node.models.ProtocolVersionInfo.InitialProtocolVersionInfo
import io.iohk.atala.prism.node.models.nodeState.LedgerData
import io.iohk.atala.prism.node.models.{ProtocolVersion, ProtocolVersionInfo}

object ProtocolVersionsDAO {

  def insertProtocolVersion(
      protocolVersion: ProtocolVersion,
      versionName: Option[String],
      effectiveSinceBlockIndex: Int,
      proposerDID: DidSuffix,
      ledgerData: LedgerData
  ): ConnectionIO[Unit] = {
    val publishedIn = ledgerData.transactionId
    val isEffective = false
    sql"""
         |INSERT INTO protocol_versions (major_version, minor_version, version_name, effective_since, published_in, is_effective, proposer_did)
         |VALUES (${protocolVersion.major}, ${protocolVersion.minor}, $versionName, $effectiveSinceBlockIndex, $publishedIn, $isEffective, $proposerDID)
       """.stripMargin.update.run.void
  }

  def getCurrentProtocolVersion: ConnectionIO[ProtocolVersion] = {
    sql"""
         |SELECT major_version, minor_version
         |FROM protocol_versions
         |WHERE is_effective
         |ORDER BY effective_since DESC
         |LIMIT 1
       """.stripMargin
      .query[ProtocolVersion]
      .option
      .map(_.getOrElse(InitialProtocolVersion))
  }

  def getLastKnownProtocolUpdate: ConnectionIO[ProtocolVersionInfo] = {
    sql"""
         |SELECT major_version, minor_version, version_name, effective_since
         |FROM protocol_versions
         |ORDER BY effective_since DESC
         |LIMIT 1
       """.stripMargin
      .query[ProtocolVersionInfo]
      .option
      .map(_.getOrElse(InitialProtocolVersionInfo))
  }

  def markEffective(
      blockIndex: Int
  ): ConnectionIO[Option[ProtocolVersionInfo]] = {
    sql"""
         |UPDATE protocol_versions
         |SET is_effective = true
         |WHERE (NOT is_effective AND $blockIndex >= effective_since)
         |RETURNING major_version, minor_version, version_name, effective_since
         |""".stripMargin.query[ProtocolVersionInfo].to[List].map {
      _.sortBy(_.effectiveSinceBlockIndex)(Ordering.Int.reverse).headOption
    }
  }

  def isProposerTrusted(didSuffix: DidSuffix): ConnectionIO[Boolean] = {
    sql"""
         |SELECT COUNT(*)
         |FROM trusted_proposers
         |WHERE did_suffix = $didSuffix
       """.stripMargin.query[Int].unique.fmap(_ > 0)
  }

  def insertTrustedProposer(didSuffix: DidSuffix): ConnectionIO[Unit] = {
    sql"""
      |INSERT INTO trusted_proposers (did_suffix)
      | VALUES ($didSuffix)
    """.stripMargin.update.run.void
  }
}
