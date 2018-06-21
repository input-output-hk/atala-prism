package io.iohk.cef

import org.scalatest.FlatSpec

class TestSpec extends FlatSpec {

  "it" should "expand" in {
    val l = for {
      messageSize <- Range.inclusive(100, 200, 25)
      loopCount <- Range.inclusive(0, 10)
    } yield messageSize

    println(l)
  }

}
