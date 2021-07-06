package io.iohk.atala.cvp.webextension.testing

import scala.scalajs.js
object FakeApis {
  def configure(): Unit = {
    js.Dynamic.global.global.chrome = FakeChromeApi
    js.Dynamic.global.global.facade = FakeCommonsFacade
  }
}
