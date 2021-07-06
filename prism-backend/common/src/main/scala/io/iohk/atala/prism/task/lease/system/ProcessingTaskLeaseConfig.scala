package io.iohk.atala.prism.task.lease.system

import com.typesafe.config.Config

case class ProcessingTaskLeaseConfig(
    leaseTimeSeconds: Int,
    extendLeaseTimeIntervalSeconds: Int,
    numberOfWorkers: Int,
    workerSleepTimeSeconds: Int
)

object ProcessingTaskLeaseConfig {
  def apply(config: Config): ProcessingTaskLeaseConfig =
    ProcessingTaskLeaseConfig(
      leaseTimeSeconds = config.getInt("leaseTimeSeconds"),
      extendLeaseTimeIntervalSeconds = config.getInt("extendLeaseTimeIntervalSeconds"),
      numberOfWorkers = config.getInt("numberOfWorkers"),
      workerSleepTimeSeconds = config.getInt("workerSleepTimeSeconds")
    )
}
