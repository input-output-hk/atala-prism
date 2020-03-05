package io.iohk.atala.cvp.webextension.testing

import scala.scalajs.js

object FakeApis {
  def configure(): Unit = {
    js.Dynamic.global.chrome = FakeChromeApi
    js.Dynamic.global.crypto = FakeCryptoApi
    js.Dynamic.global.facade = FakeCommonsFacade
  }
}
