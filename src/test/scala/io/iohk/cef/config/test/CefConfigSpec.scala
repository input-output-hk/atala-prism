package io.iohk.cef.config
import org.scalatest.FlatSpec
import pureconfig.generic.auto._
import ConfigReaderExtensions._

class CefConfigSpec extends FlatSpec {
  behavior of "CefConfig"

  it should "be read from a valid configuration file" in {
    val config = pureconfig.loadConfigOrThrow[CefConfig]
    println(config)
  }
}
