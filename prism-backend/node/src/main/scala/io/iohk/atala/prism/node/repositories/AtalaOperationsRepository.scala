package io.iohk.atala.prism.node.repositories

import cats.effect.BracketThrow
import cats.syntax.traverse._
import derevo.derive
import derevo.tagless.applyK
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.iohk.atala.prism.connector.AtalaOperationId
import io.iohk.atala.prism.node.errors.NodeError
import io.iohk.atala.prism.node.models.{
  AtalaObjectId,
  AtalaObjectInfo,
  AtalaObjectTransactionSubmission,
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
  def insertObjectAndOperations(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationIds: List[AtalaOperationId],
      atalaOperationsStatus: AtalaOperationStatus
  ): F[Either[NodeError, (Int, Int)]]

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): F[Either[NodeError, Unit]]

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]]

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): F[List[Option[AtalaObjectInfo]]]
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

  def insertObjectAndOperations(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationIds: List[AtalaOperationId],
      atalaOperationsStatus: AtalaOperationStatus
  ): F[Either[NodeError, (Int, Int)]] = {
    val atalaOperationData = atalaOperationIds.map((_, objectId, atalaOperationsStatus))

    val query = for {
      numInsertObject <- AtalaObjectsDAO.insert(AtalaObjectCreateData(objectId, objectBytes))
      numInsertOperations <- AtalaOperationsDAO.insertMany(atalaOperationData)
    } yield (numInsertObject, numInsertOperations)

    val opDescription = s"inserting object and operations \n Operations:[${atalaOperationIds.mkString("\n")}]"
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
      _ <- AtalaObjectsDAO.setProcessedBatch(oldObjects.map(_.objectId))
    } yield ()

    val opDescription = s"record new Atala Object ${atalaObject.objectId}"
    connectionIOSafe(query.logSQLErrors(opDescription, logger)).transact(xa)
  }

  def getNotPublishedObjects: F[Either[NodeError, List[AtalaObjectInfo]]] = {
    val query = for {
      objectIds <- AtalaObjectsDAO.getNotPublishedObjectIds
      objectInfos <- objectIds.traverse(AtalaObjectsDAO.get)
    } yield objectInfos.flatten

    val opDescription = "Extract not submitted objects."
    connectionIOSafe(query.logSQLErrors(opDescription, logger)).transact(xa)
  }

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): F[List[Option[AtalaObjectInfo]]] =
    transactions.traverse { transaction =>
      val query = AtalaObjectsDAO.get(transaction.atalaObjectId)

      val opDescription = s"Getting atala object by atalaObjectId = ${transaction.atalaObjectId}"
      connectionIOSafe(query.logSQLErrors(opDescription, logger))
        .map {
          case Left(err) =>
            logger.error(s"Could not retrieve atala object ${transaction.atalaObjectId}", err)
            None
          case Right(None) =>
            logger.error(s"Atala object ${transaction.atalaObjectId} not found")
            None
          case Right(result) =>
            result
        }
        .transact(xa)
    }
}

private final class AtalaOperationsRepositoryMetrics[F[_]: TimeMeasureMetric: BracketThrow]
    extends AtalaOperationsRepository[Mid[F, *]] {

  private val repoName = "AtalaOperationsRepository"

  private lazy val insertObjectAndOperationsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "insertObjectAndOperationsTimer")

  private lazy val mergeObjectsTimerTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "mergeObjectsTimer")

  private lazy val getNotPublishedObjectsTimer =
    TimeMeasureUtil.createDBQueryTimer(repoName, "getNotPublishedObjects")

  private lazy val retrieveObjectsTimer = TimeMeasureUtil.createDBQueryTimer(repoName, "retrieveObjects")

  def insertObjectAndOperations(
      objectId: AtalaObjectId,
      objectBytes: Array[Byte],
      atalaOperationIds: List[AtalaOperationId],
      atalaOperationsStatus: AtalaOperationStatus
  ): Mid[F, Either[NodeError, (Int, Int)]] = _.measureOperationTime(insertObjectAndOperationsTimer)

  def updateMergedObjects(
      atalaObject: AtalaObjectInfo,
      operations: List[SignedAtalaOperation],
      oldObjects: List[AtalaObjectInfo]
  ): Mid[F, Either[NodeError, Unit]] = _.measureOperationTime(mergeObjectsTimerTimer)

  def getNotPublishedObjects: Mid[F, Either[NodeError, List[AtalaObjectInfo]]] =
    _.measureOperationTime(getNotPublishedObjectsTimer)

  def retrieveObjects(transactions: List[AtalaObjectTransactionSubmission]): Mid[F, List[Option[AtalaObjectInfo]]] =
    _.measureOperationTime(retrieveObjectsTimer)
}
