package io.iohk.atala.cvp.webextension.testing

import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

object WalletTestHelper {
  val PASSWORD = "test-password"
  val TEST_KEY = "test-key"
  val ORGANISATION_NAME = "IOHK"

  def setUpWallet(f: => Future[Unit]): Future[Unit] = {
    f
  }

  def withWallet(test: => Future[Assertion]): Future[Assertion] = {
    try {
      test
    } finally {
      println("Test complete")
    }
  }

  def futureResult(assert: => Assertion)(implicit ec: ExecutionContext): Future[Assertion] = {
    val promise = Promise[Boolean]()
    js.timers.setTimeout(10) {
      promise.success(true)
    }
    promise.future.map(_ => {
      assert
    })
  }

}
