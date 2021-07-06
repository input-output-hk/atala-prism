package io.iohk.atala.prism.task.lease.system

import monix.eval.Task

trait ProcessingTaskProcessor[S <: ProcessingTaskState] {
  def process(processingTask: ProcessingTask[S]): Task[ProcessingTaskResult[S]]
}
