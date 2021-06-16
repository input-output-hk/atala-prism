package io.iohk.atala.prism.task.lease.system

import cats.data.EitherT
import io.circe.Decoder
import io.iohk.atala.prism.errors.PrismError
import io.iohk.atala.prism.services.ConnectorClientService
import monix.eval.Task
import org.slf4j.Logger

import java.time.Instant
import java.time.temporal.ChronoUnit
import cats.implicits._

object ProcessingTaskProcessorOps {
  implicit class ProcessingTasksProcessorOps[A](task: Task[Either[PrismError, A]]) {
    def sendResponseOnError(
        connectorService: ConnectorClientService,
        receivedMessageId: String,
        connectionId: String
    ): Task[Either[PrismError, A]] = {
      task.flatTap {
        case Right(_) => Task.unit
        case Left(prismError: PrismError) =>
          connectorService
            .sendResponseMessage(prismError.toAtalaMessage, receivedMessageId, connectionId)
            .void
      }
    }

    def logErrorIfPresent(implicit logger: Logger): Task[Either[PrismError, A]] = {
      task.map(_.leftMap { error =>
        logger.info(s"Failed processing task: ${error.toStatus.getDescription}")
        error
      })
    }

    def mapErrorToProcessingTaskFinished[S <: ProcessingTaskState](): Task[Either[ProcessingTaskResult[S], A]] = {
      task.map(_.leftMap(_ => ProcessingTaskResult.ProcessingTaskFinished))
    }

    def mapErrorToProcessingTaskScheduled[S <: ProcessingTaskState](
        processingTask: ProcessingTask[S],
        delayInSeconds: Long = 30
    ): Task[Either[ProcessingTaskResult[S], A]] =
      mapErrorToProcessingTaskScheduled(processingTask, Instant.now().plus(delayInSeconds, ChronoUnit.SECONDS))

    def mapErrorToProcessingTaskScheduled[S <: ProcessingTaskState](
        processingTask: ProcessingTask[S],
        scheduledTime: Instant
    ): Task[Either[ProcessingTaskResult[S], A]] =
      mapErrorToProcessingTaskScheduled(processingTask.state, processingTask.data, scheduledTime)

    def mapErrorToProcessingTaskScheduled[S <: ProcessingTaskState](
        state: S,
        data: ProcessingTaskData,
        scheduledTime: Instant
    ): Task[Either[ProcessingTaskResult[S], A]] = {
      task.map(_.leftMap(_ => ProcessingTaskResult.ProcessingTaskScheduled[S](state, data, scheduledTime)))
    }
  }

  def parseProcessingTaskData[A: Decoder, S <: ProcessingTaskState](
      processingTask: ProcessingTask[S]
  )(implicit logger: Logger): EitherT[Task, ProcessingTaskResult[S], A] =
    EitherT.fromEither[Task](processingTask.data.json.as[A].left.map { decodingFailure =>
      logger.error(
        s"Cannot decode task data: ${processingTask.data.json.toString()}, failure reason: ${decodingFailure.message}"
      )
      ProcessingTaskResult.ProcessingTaskFinished
    })

}
