package io.iohk.cef.ledger

case class StateM[S, A](f: S => (S, A)) extends (S => (S, A)) {

  override def apply(v1: S): (S, A) = f(v1)

  def flatMap[B](g: A => StateM[S, B]): StateM[S, B] = {
    StateM(s => {
      val (s1, a) = f(s)
      g(a)(s1)
    })
  }

  def map[B](g: A => B): StateM[S, B] = {
    StateM(s => {
      val (s1, a) = f(s)
      (s1, g(a))
    })
  }
}
