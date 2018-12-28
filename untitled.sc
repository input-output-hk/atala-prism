
type Nel[A] = ::[A]

object Nel {
  def apply[A](h: A): Nel[A] = new Nel(h, Nil)
  def apply[A](h: A, t: List[A]): Nel[A] = new Nel(h, t)
}

import Nel._

def f(nel: Nel[Int]) = nel.mkString(" ")

println(f(Nel(5)))
println(f(Nel(5, List(6, 7, 8))))
