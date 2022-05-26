package io.iohk.atala.prism.node.repositories

import cats.{Applicative, Comonad, Functor}
import cats.effect.Resource
import cats.syntax.comonad._
import cats.syntax.functor._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models._
import io.iohk.atala.prism.node.repositories.daos.{AtalaObjectsDAO, AtalaOperationsDAO}
import io.iohk.atala.prism.node.repositories.daos.AtalaObjectsDAO.AtalaObjectCreateData
import io.iohk.atala.prism.utils.syntax.DBConnectionOps
import io.iohk.atala.prism.metrics.TimeMeasureMetric
import io.iohk.atala.prism.node.repositories.logs.AtalaOperationsRepositoryLogs
import io.iohk.atala.prism.node.repositories.metrics.AtalaOperationsRepositoryMetrics
import io.iohk.atala.prism.node.repositories.utils.connectionIOSafe
import io.iohk.atala.prism.protos.node_models.SignedAtalaOperation
import tofu.higherKind.Mid
import tofu.logging.{Logs, ServiceLogging}
import tofu.syntax.monoid.TofuSemigroupOps
import cats.effect.MonadCancelThrow
import io.iohk.atala.prism.models.AtalaOperationId

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

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): F[Either[NodeError, Option[AtalaOperationInfo]]]

  def getOperationsCount(status: AtalaOperationStatus): F[Either[NodeError, Int]]
}

object AtalaOperationsRepository {
  def apply[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): R[AtalaOperationsRepository[F]] =
    for {
      serviceLogs <- logs.service[AtalaOperationsRepository[F]]
    } yield {
      implicit val implicitLogs: ServiceLogging[F, AtalaOperationsRepository[F]] = serviceLogs
      val metrics: AtalaOperationsRepository[Mid[F, *]] =
        new AtalaOperationsRepositoryMetrics[F]()
      val logs: AtalaOperationsRepository[Mid[F, *]] =
        new AtalaOperationsRepositoryLogs[F]
      val mid = metrics |+| logs
      mid attach new AtalaOperationsRepositoryImpl[F](transactor)
    }

  def resource[F[_]: MonadCancelThrow: TimeMeasureMetric, R[
      _
  ]: Applicative: Functor](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): Resource[R, AtalaOperationsRepository[F]] =
    Resource.eval(AtalaOperationsRepository(transactor, logs))

  def unsafe[F[_]: MonadCancelThrow: TimeMeasureMetric, R[_]: Comonad](
      transactor: Transactor[F],
      logs: Logs[R, F]
  ): AtalaOperationsRepository[F] =
    AtalaOperationsRepository(transactor, logs).extract
}

private final class AtalaOperationsRepositoryImpl[F[_]: MonadCancelThrow](
    xa: Transactor[F]
) extends AtalaOperationsRepository[F] {
  def insertOperation(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationId: AtalaOperationId,
      atalaOperationStatus: AtalaOperationStatus
  ): F[Either[NodeError, (Int, Int)]] = {
    val query = for {
      numInsertObject <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(objectId, objectBytes, AtalaObjectStatus.Scheduled)
      )
      numInsertOperations <- AtalaOperationsDAO.insert(
        (atalaOperationId, objectId, atalaOperationStatus)
      )
    } yield (numInsertObject, numInsertOperations)

    val opDescription = s"inserting operation: [$atalaOperationId]"
    connectionIOSafe(query.logSQLErrorsV2(opDescription)).transact(xa)
  }

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): F[Either[NodeError, Unit]] = {
    val query = for {
      _ <- AtalaObjectsDAO.insert(
        AtalaObjectCreateData(atalaObject.objectId, atalaObject.byteContent, AtalaObjectStatus.Pending)
      )
      _ <- AtalaOperationsDAO.updateAtalaOperationObjectBatch(
        operations.map(AtalaOperationId.of),
        atalaObject.objectId
      )
      _ <- AtalaObjectsDAO.updateObjectStatusBatch(
        oldObjects.map(_.objectId),
        AtalaObjectStatus.Merged
      )
    } yield ()

    val opDescription = s"record new Atala Object ${atalaObject.objectId}"
    connectionIOSafe(query.logSQLErrorsV2(opDescription)).transact(xa)
  }

  def getOperationInfo(
      atalaOperationId: AtalaOperationId
  ): F[Either[NodeError, Option[AtalaOperationInfo]]] = {
    val opDescription = s"getting operation info for [$atalaOperationId]"
    val query = AtalaOperationsDAO
      .getAtalaOperationInfo(atalaOperationId)
      .logSQLErrorsV2(opDescription)

    connectionIOSafe(query).transact(xa)
  }

  def getOperationsCount(status: AtalaOperationStatus): F[Either[NodeError, Int]] = {
    val opDescription = s"getting operations count"
    val query = AtalaOperationsDAO.getAtalaOperationsCount(status).logSQLErrorsV2(opDescription)

    connectionIOSafe(query).transact(xa)
  }
}
