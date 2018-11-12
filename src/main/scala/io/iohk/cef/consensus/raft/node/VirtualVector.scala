package io.iohk.cef.consensus.raft.node

import scala.collection._
import generic._
import mutable.Builder

/**
  * The scaladocs state that
  * Indexed sequences support constant-time or near constant-time element access and length computation.
  * They are defined in terms of abstract methods apply for indexing and length.
  * It seems magic that it's possible to define a new collection implementation with just two methods!
  * @param base    the base collection.
  * @param deletes an int defining a number of deletes from the tail end.
  * @param writes a Seq of additional entries to append.
  * @tparam T any type.
  */
class VirtualVector[+T](base: IndexedSeq[T], deletes: Int, writes: Seq[T])
    extends IndexedSeq[T]
    with GenericTraversableTemplate[T, VirtualVector] {
  override def companion: GenericCompanion[VirtualVector] = VirtualVector
  override def length: Int = base.size - deletes + writes.size
  override def apply(idx: Int): T = {
    if (idx < base.size - deletes) {
      base(idx)
    } else {
      writes(idx - base.size + deletes)
    }
  }
}

object VirtualVector extends IndexedSeqFactory[VirtualVector] {
  // A single CBF which can be checked against to identify
  // an indexed collection type.
  override val ReusableCBF: GenericCanBuildFrom[Nothing] = new GenericCanBuildFrom[Nothing] {
    override def apply() = newBuilder[Nothing]
  }
  def newBuilder[T]: Builder[T, VirtualVector[T]] = new Builder[T, VirtualVector[T]] {
    private val internal = immutable.IndexedSeq.newBuilder[T]
    def clear(): Unit = internal.clear()
    def result(): VirtualVector[T] = new VirtualVector(internal.result(), 0, Seq.empty)
    def +=(elem: T): this.type = {
      internal += elem
      this
    }
  }
  implicit def canBuildFrom[T]: CanBuildFrom[Coll, T, VirtualVector[T]] =
    ReusableCBF.asInstanceOf[GenericCanBuildFrom[T]]
}
