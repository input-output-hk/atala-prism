package io.iohk.cef.data.query

trait Queriable[T] {
  val fieldCount: Int
  def applyPredicate(t: T, index: Int, value: String): Boolean
}
