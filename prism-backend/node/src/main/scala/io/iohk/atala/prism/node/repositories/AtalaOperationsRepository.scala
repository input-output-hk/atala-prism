package io.iohk.atala.prism.node.repositories

import cats.effect.BracketThrow
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectStatus,
  AtalaOperationInfo,
  AtalaOperationStatus
}
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectsDAO, AtalaOperationsDAO}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.{TimeMeasureMetric, TimeMeasureUtil}
import io.iohk.atala.prism.metrics.TimeMeasureUtil.MeasureOps
import io.iohk.atala.prism.node.repositories.utils.connectionIOSafe
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import org.slf4j.{Logger, LoggerFactory}
import tofu.higherKind.Mid

@derive(applyK)
trait AtalaOperationsRepository[F[_]] {
  def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: AtalaOperationStatus
  ): F[Either[NodeError, (Int, Int)]]

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): F[Either[NodeError, Unit]]

  def getOperationInfo(atalaOperationId: AtalaOperationId): F[Option[AtalaOperationInfo]]
}

object AtalaOperationsRepository {
  def apply[F[_]: TimeMeasureMetric: BracketThrow](transactor: Transactor[F]): AtalaOperationsRepository[F] = {
    val metrics: AtalaOperationsRepository[Mid[F, *]] = new AtalaOperationsRepositoryMetrics[F]()
    metrics attach new AtalaOperationsRepositoryImpl[F](transactor)
  }
}

private final class AtalaOperationsRepositoryImpl[F[_]: BracketThrow](xa: Transactor[F])
    extends AtalaOperationsRepository[F] {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: AtalaOperationStatus
  ): F[Either[NodeError, (Int, Int)]] = {
    val query = for {
      numInsertObject <- AtalaObjectsDAO.insert(AtalaObjectCreateData(objectId, objectBytes))
      numInsertOperations <- AtalaOperationsDAO.insert((atalaOperationId, objectId, atalaOperationStatus))
    } yield (numInsertObject, numInsertOperations)

    val opDescription = s"inserting operation: [$atalaOperationId]"
    connectionIOSafe(query.logSQLErrors(opDescription, logger)).transact(xa)
  }

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): F[Either[NodeError, Unit]] = {
    val query = for {
      _ <- AtalaObjectsDAO.insert(AtalaObjectCreateData(atalaObject.objectId, atalaObject.byteContent))
      _ <- AtalaOperationsDAO.updateAtalaOperationObjectBatch(
        operations.map(AtalaOperationId.of),
        atalaObject.objectId
      )
      _ <- AtalaObjectsDAO.updateObjectStatusBatch(oldObjects.map(_.objectId), AtalaObjectStatus.Merged)
    } yield ()

    val opDescription = s"record new Atala Object ${atalaObject.objectId}"
    connectionIOSafe(query.logSQLErrors(opDescription, logger)).transact(xa)
  }

  def getOperationInfo(atalaOperationId: AtalaOperationId): F[Option[AtalaOperationInfo]] = {
    val opDescription = s"getting operation info for [$atalaOperationId]"
    val query = AtalaOperationsDAO.getAtalaOperationInfo(atalaOperationId).logSQLErrors(opDescription, logger)

    connectionIOSafe(query)
      .map(
        _.left
          .map { err =>
            logger.error(s"Could not retrieve operation [$atalaOperationId]", err)
          }
          .getOrElse(None)
      )
      .transact(xa)
  }
}

private final class AtalaOperationsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends AtalaOperationsRepository[Mid[F, *]] {
  private val repoName = "AtalaOperationsRepository"

  private lazy val insertObjectAndOperationsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "insertObjectAndOperations")

  private lazy val mergeObjectsTimerTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "mergeObjects")

  private lazy val getOperationInfoTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "getOperationInfo")

  def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationIds: AtalaOperationId,
      atalaOperationsStatus: AtalaOperationStatus
  ): Mid[F, Either[NodeError, (Int, Int)]] = _.measureOperationTime(insertObjectAndOperationsTimer)

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): Mid[F, Either[NodeError, Unit]] = _.measureOperationTime(mergeObjectsTimerTimer)

  def getOperationInfo(atalaOperationId: AtalaOperationId): Mid[F, Option[AtalaOperationInfo]] =
    _.measureOperationTime(getOperationInfoTimer)
}
