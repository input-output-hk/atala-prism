package io.iohk.cef.utils

object EitherTransforms {

  implicit class SeqEitherHelpers[L, R](eithers: Seq[Either[L, R]]) {
    def toEitherList: Either[L, Seq[R]] = eithers match {
      case Seq() => Right(Seq())
      case head +: tail =>
        tail.foldLeft[Either[L, Seq[R]]](head.right.map(Seq(_)))((state, current) => {
          state.flatMap(s => current.right.map(_ +: s))
        })
    }
  }
}
