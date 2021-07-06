package io.iohk.atala.prism.task.lease.system

import enumeratum.EnumEntry

abstract class ProcessingTaskState(value: String) extends EnumEntry {
  override def entryName: String = value
}
