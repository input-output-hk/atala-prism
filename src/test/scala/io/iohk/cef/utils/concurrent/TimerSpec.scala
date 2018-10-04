package io.iohk.cef.utils.concurrent

import java.lang.System.currentTimeMillis

import org.scalatest.FlatSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.{Await, TimeoutException}
import scala.concurrent.duration._

class TimerSpec extends FlatSpec {
  
  behavior of "Timer"

  it should "run a task after a delay" in {
    val t1 = currentTimeMillis()

    Timer
      .schedule(50 millis) {
        val t2 = currentTimeMillis()
        (t2 - t1).toInt should be >= 50
      }
      .futureValue
  }

  it should "cancel a task" in {
    val c = Timer
      .schedule(50 millis) {
        println("Task completed. Oh dear!")
      }

    c.cancel()

    a[TimeoutException] should be thrownBy Await.result(c, 100 millis)
  }
}
