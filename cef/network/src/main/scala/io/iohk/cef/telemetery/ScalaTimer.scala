package io.iohk.cef.telemetery

import java.util.concurrent.Callable

import io.micrometer.core.instrument.Timer

class ScalaTimer(timer: Timer) {

  def wrap[T](block: () => T) = {
    timer.wrap(new Callable[T] {
      override def call(): T = block()
    })
  }
}

object ScalaTimer {
  implicit class TimerOps(timer: Timer) {
    def asScala = new ScalaTimer(timer)
  }
}
