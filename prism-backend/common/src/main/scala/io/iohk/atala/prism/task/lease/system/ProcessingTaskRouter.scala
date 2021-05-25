package io.iohk.atala.prism.task.lease.system

import io.iohk.atala.prism.task.lease.system.ProcessingTaskState._
import io.iohk.atala.prism.task.lease.system.processors.TestState1Processor
import monix.eval.Task

trait ProcessingTaskRouter {
  def process(processingTask: ProcessingTask): Task[ProcessingTaskResult]
}

class ProcessingTaskRouterImpl(processingTaskService: ProcessingTaskService) extends ProcessingTaskRouter {

  override def process(processingTask: ProcessingTask): Task[ProcessingTaskResult] =
    processingTask.state match {
      case TestState1 => TestState1Processor.process(processingTask, processingTaskService)
    }

}
