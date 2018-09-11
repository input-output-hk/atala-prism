package io.iohk.cef.consensus.raft.node

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
class VirtualVector[T](base: IndexedSeq[T], deletes: Int, writes: Seq[T]) extends IndexedSeq[T] {
  override def length: Int = base.size - deletes + writes.size
  override def apply(idx: Int): T = {
    if (idx < base.size - deletes) {
      base(idx)
    } else {
      writes(idx - base.size + deletes)
    }
  }
}
