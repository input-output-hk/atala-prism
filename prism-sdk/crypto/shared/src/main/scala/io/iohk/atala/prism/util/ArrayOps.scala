package io.iohk.atala.prism.util

/**
  * Array operations avoiding instantiation of ClassTag
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

    /**
      * Convert [[Seq]] to byte array without a [[scala.reflect.ClassTag]] dependency.
      */
    def toByteArray: Array[Byte] = {
      val arr = new Array[Byte](seq.size)

      for ((b, i) <- seq.zipWithIndex) {
        arr(i) = b
      }
      arr
    }
  }

  implicit class ByteArrayOps(val array: Array[Byte]) {

    /**
      * Prepend element to the array without a [[scala.reflect.ClassTag]] dependency.
      */
    def safePrepended(x: Byte): Array[Byte] = {
      val dest = new Array[Byte](array.length + 1)
      System.arraycopy(array, 0, dest, 1, array.length)
      dest(0) = x
      dest
    }

    /**
      * Concat two arrays without a [[scala.reflect.ClassTag]] dependency.
      *
      * @param suffix
      * @return
      */
    def safeAppendedAll(suffix: Array[Byte]): Array[Byte] = {
      val dest = new Array[Byte](array.length + suffix.length)
      System.arraycopy(array, 0, dest, 0, array.length)
      System.arraycopy(suffix, 0, dest, array.length, suffix.length)
      dest
    }

    /**
      * Copies the specified range of the array into a new array.
      */
    def safeCopyOfRange(from: Int, to: Int): Array[Byte] = {
      val newLength = to - from
      val dest = new Array[Byte](newLength)
      System.arraycopy(array, from, dest, 0, Math.min(array.length - from, newLength))
      dest
    }
  }
}
