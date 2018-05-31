package io.iohk.cef.utils

import scala.util.Random

object RandomElement {
  def randomElement[T](s: Set[T]): T = {
    val n = Random.nextInt(s.size)
    s.iterator.drop(n).next
  }

  def randomElement[T](s: Seq[T]): T = {
    val n = Random.nextInt(s.size)
    s(n)
  }
}
