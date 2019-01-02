package io.iohk.cef

package object data {

  type DataItemId = String
  type TableId = String

  type NonEmptyList[A] = ::[A]

  object NonEmptyList {

    def apply[A](h: A, t: List[A]): NonEmptyList[A] = new NonEmptyList(h, t)

    def apply[A](h: A, t: A*): NonEmptyList[A] = new NonEmptyList(h, t.toList)

    def from[A](list: List[A]): Option[NonEmptyList[A]] = list match {
      case x :: xs => Option(new NonEmptyList(x, xs))
      case Nil => Option.empty
    }
  }
}
