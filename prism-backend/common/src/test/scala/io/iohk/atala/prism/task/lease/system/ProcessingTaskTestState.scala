package io.iohk.atala.prism.task.lease.system

import enumeratum.Enum

sealed abstract class ProcessingTaskTestState(value: String) extends ProcessingTaskState(value)

object ProcessingTaskTestState extends Enum[ProcessingTaskTestState] {
  lazy val values = findValues

  final case object TestState1 extends ProcessingTaskTestState("TEST_STATE_1")
  final case object TestState2 extends ProcessingTaskTestState("TEST_STATE_2")
}
