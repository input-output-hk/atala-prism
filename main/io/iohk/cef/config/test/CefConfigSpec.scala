package io.iohk.cef.config.test

import io.iohk.cef.config.CefConfig
import org.scalatest.FlatSpec
import pureconfig.generic.auto._
import io.iohk.cef.config.ConfigReaderExtensions._

class CefConfigSpec extends FlatSpec {
  behavior of "CefConfig"

  it should "be read from a valid configuration file" in {
    pureconfig.loadConfigOrThrow[CefConfig]
  }
}
