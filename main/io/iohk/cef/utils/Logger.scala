package io.iohk.cef.utils

import org.slf4j.LoggerFactory

trait Logger {
  val log = LoggerFactory.getLogger(getClass)
}
