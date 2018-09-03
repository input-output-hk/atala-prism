package io.iohk.cef.consensus.raft

object IdentityMonad {

  //  type Id[A] = A
  //  implicit val Id = {
  //    def pure[A](a: A): A = a
  //
  //    def extract[A](a: A): A = a
  //
  //    def flatMap[A, B](a: A)(f: A => B): B = f(a)
  //
  //    def map[A, B](fa: A)(f: A => B): B = f(fa)
  //
  //    def lift[A, B](f: A => B): A => B = f
  //
  //    def foldLeft[A, B](a: A, b: B)(f: (B, A) => B) = f(b, a)
  //  }

  //  type Id[T] = T
  //
  object Id {
    def pure[A](a: A): A = a

    def get[A](a: A): A = a

    def flatMap[A, B](a: A)(f: A => Id[B]): Id[B] = f(a)

    def map[A, B](a: A)(f: A => B): Id[B] = f(a)

    def lift[A, B](f: A => B): A => B = f

    def foldLeft[A, B](a: A, b: B)(f: (B, A) => B): B = f(b, a)
  }

  implicit class Id[+A](a: A) {
    def get: A = Id.get(a)

    def map[B](f: A => B): Id[B] = Id.map(a)(f)

    def flatMap[B](f: A => Id[B]): Id[B] = Id.flatMap(a)(f)

    def foldLeft[B](b: B)(f: (B, A) => B): B = f(b, a)
  }

}
