package io.iohk.atala.prism.task.lease.system

import enumeratum.{Enum, EnumEntry}

sealed abstract class ProcessingTaskState(value: String) extends EnumEntry {
  override def entryName: String = value
}
object ProcessingTaskState extends Enum[ProcessingTaskState] {
  lazy val values = findValues

  final case object TestState1 extends ProcessingTaskState("TEST_STATE_1")
}
