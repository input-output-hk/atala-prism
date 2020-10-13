package io.iohk.atala.util

/** Array operations avoiding instantiation of ClassTag
  *
  * Most Scala standard library collection operations involving creation of new array take ClassTag as an implicit
  * argument, as this is what they use to know how to create new Array[T] - array work a little different than
  * generic classes and don't have element type erased.
  *
  * Utility operations defined here provide a way to execute common operations without instantiating ClassTag,
  * as using one causes runtime exception on Android.
  */
object ArrayOps {
  implicit class SeqByteArrayOps(seq: Seq[Byte]) {
    def toByteArray: Array[Byte] = {
      val arr = new Array[Byte](seq.size)

      for ((b, i) <- seq.zipWithIndex) {
        arr(i) = b
      }
      arr
    }
  }
}
