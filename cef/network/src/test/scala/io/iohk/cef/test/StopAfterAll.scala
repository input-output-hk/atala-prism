package io.iohk.cef.test

import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

trait StopAfterAll extends BeforeAndAfterAll {
  this: TestKit with FlatSpecLike =>

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }
}
