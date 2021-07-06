package io.iohk.atala.prism.task.lease.system

import monix.eval.Task

trait ProcessingTaskRouter[S <: ProcessingTaskState] {
  def process(processingTask: ProcessingTask[S], workerNumber: Int): Task[ProcessingTaskResult[S]]
}
