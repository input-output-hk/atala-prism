package io.iohk.atala.prism.node.repositories

import cats.data.EitherT
import cats.effect.BracketThrow
import cats.syntax.applicative._
import cats.syntax.either._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.kotlin.identity.{DID, DIDSuffix}
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.errors.NodeError.UnknownValueError
import io.iohk.atala.prism.node.models.nodeState.DIDDataState
import io.iohk.atala.prism.node.repositories.daos.{DIDDataDAO, PublicKeysDAO}
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait DIDDataRepository[F[_]] {
  def findByDid(did: DID): F[Either[NodeError, Option[DIDDataState]]]
}

object DIDDataRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): DIDDataRepository[F] = {
    val metrics: DIDDataRepository[Mid[F, *]] = new DIDDataRepositoryMetrics[F]()
    metrics attach new DIDDataRepositoryImpl[F](transactor)
  }
}

private final class DIDDataRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F]) extends DIDDataRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def findByDid(did: DID): F[Either[NodeError, Option[DIDDataState]]] =
    Option(did.getCanonicalSuffix)
      .fold[F[Either[NodeError, Option[DIDDataState]]]](logDidAndReturnUnknownValue(did))(getByCanonicalSuffix)

  private def logDidAndReturnUnknownValue(did: DID): F[Either[NodeError, Option[DIDDataState]]] = {
    logger.info(s"Unknown DID format: $did")
    Either.left[NodeError, Option[DIDDataState]](UnknownValueError("did", did.getValue)).pure[F]
  }

  private def getByCanonicalSuffix(canonicalSuffix: DIDSuffix): F[Either[NodeError, Option[DIDDataState]]] = {
    val query = for {
      lastOperationMaybe <- DIDDataDAO.getLastOperation(canonicalSuffix)
      keys <- PublicKeysDAO.findAll(canonicalSuffix)
    } yield lastOperationMaybe map { lastOperation =>
      DIDDataState(canonicalSuffix, keys, lastOperation)
    }

    EitherT
      .right[NodeError](query)
      .value
      .logSQLErrors(s"finding, did suffix - $canonicalSuffix", logger)
      .transact(xa)
  }
}

private final class DIDDataRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends DIDDataRepository[Mid[F, *]] {
  private lazy val findByDidTimer = TimeMeasureUtil.createDBQueryTimer("DIDDataRepository", "findByDid")
  override def findByDid(did: DID): Mid[F, Either[NodeError, Option[DIDDataState]]] =
    _.measureOperationTime(findByDidTimer)
}
