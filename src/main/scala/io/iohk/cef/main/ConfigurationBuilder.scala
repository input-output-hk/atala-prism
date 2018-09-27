package io.iohk.cef.main
import java.time.Clock

import scala.concurrent.duration.{Duration, FiniteDuration}

trait ConfigurationBuilder {
  val maxBlockSizeInBytes: Int
  val defaultTransactionExpiration: Duration
  val clock: Clock

  val blockCreatorInitialDelay: FiniteDuration
  val blockCreatorInterval: FiniteDuration

}
